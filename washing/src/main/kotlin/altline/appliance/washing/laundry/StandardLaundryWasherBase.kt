package altline.appliance.washing.laundry

import altline.appliance.common.Body
import altline.appliance.common.RefreshPeriod
import altline.appliance.common.SpeedModifier
import altline.appliance.electricity.ElectricalDevice
import altline.appliance.electricity.transit.BasicElectricalConduit
import altline.appliance.electricity.transit.ElectricalDrainPort
import altline.appliance.measure.*
import altline.appliance.measure.Temperature.Companion.celsius
import altline.appliance.measure.Volume.Companion.liters
import altline.appliance.spin.ElectricMotor
import altline.appliance.spin.SpinDirection
import altline.appliance.substance.transit.BasicSubstanceConduit
import altline.appliance.substance.transit.ElectricPump
import altline.appliance.substance.transit.SubstanceConduit
import altline.appliance.washing.Washer
import altline.appliance.washing.laundry.washCycle.CentrifugeParams
import altline.appliance.washing.laundry.washCycle.LaundryWashCycle
import altline.appliance.washing.laundry.washCycle.WashParams
import altline.appliance.washing.laundry.washCycle.phase.SpinPhase
import io.nacular.measured.units.*
import io.nacular.measured.units.Time.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

abstract class StandardLaundryWasherBase(
    protected open val controller: LaundryWasherController,
    protected open val dispenser: LaundryWashDispenser,
    protected open val drum: Drum,
    protected open val drumMotor: ElectricMotor,
    protected open val pump: ElectricPump,
    protected open val config: LaundryWasherConfig
) : Washer, ElectricalDevice {

    private val machineScope = CoroutineScope(Dispatchers.Default)

    override val power: Measure<Power>
        get() = TODO("Not yet implemented")

    private val powerInletConduit by lazy {
        BasicElectricalConduit(
            maxFlowRate = controller.power + drum.heater.power + drumMotor.power + pump.power,
            inputCount = 1,
            outputCount = 4
        ).apply {
            outputs[0] connectTo controller.powerInlet
            outputs[1] connectTo drum.heater.powerInlet
            outputs[2] connectTo drumMotor.powerInlet
            outputs[3] connectTo pump.powerInlet
        }
    }

    override val powerInlet: ElectricalDrainPort
        get() = powerInletConduit.inputs[0]

    open val fluidIntake: SubstanceConduit by lazy {
        BasicSubstanceConduit(config.intakeFlowRate)
    }

    open val fluidOutlet: SubstanceConduit by lazy {
        BasicSubstanceConduit(config.outputFlowRate)
    }

    private val thermostat = MeasuringTrigger<Temperature>(
        triggerSetting = 20 * celsius,
        tolerance = 5 * celsius,
        onDropBelow = { drum.heater.start() },
        onRiseAbove = { drum.heater.stop() }
    )

    val poweredOn: Boolean
        get() = controller.poweredOn

    override val running: Boolean
        get() = controller.cycleRunning

    val paused: Boolean
        get() = controller.cyclePaused

    val runningTime: Measure<Time>?
        get() = controller.cycleRunningTime

    var doorLocked: Boolean = false
        private set

    override val load: Set<Body>
        get() = drum.load

    val washCycles: List<LaundryWashCycle>
        get() = controller.washCycles

    var selectedWashCycle: LaundryWashCycle
        get() = controller.selectedWashCycle
        set(value) {
            controller.selectedWashCycle = value
        }

    val activeWashCycle: LaundryWashCycle?
        get() = controller.activeWashCycle

    private var scanningJob: Job? = null
    var scanner: WasherStateScanner? = null
        set(value) {
            if (value != field) {
                field = value
                scanningJob?.cancel()
                scanningJob =
                    if (value != null) machineScope.launch { startScan() }
                    else null
            }
        }

    /**
     * A short-running job that operates after a cycle is paused of stopped to ensure the right conditions.
     */
    private var sideJob: Job? = null

    private suspend fun startScan() {
        scanner?.washLiquid = drum.excessLiquid

        repeatPeriodically(RefreshPeriod) {
            scanner?.let { scanner ->
                baseScan(scanner)
                scanState(scanner)
            }
        }
    }

    private fun baseScan(scanner: WasherStateScanner) {
        with(scanner) {
            scanDispenser(dispenser)
            scanDrainPump(pump)
            scanDrumMotor(drumMotor)
        }
    }

    protected abstract fun scanState(scanner: WasherStateScanner)

    override fun load(vararg items: Body) {
        if (!doorLocked) drum.load(*items)
    }

    override fun unload(vararg items: Body) {
        if (!doorLocked) drum.unload(*items)
    }

    override fun unloadAll(): List<Body> {
        return if (!doorLocked) drum.unloadAll() else emptyList()
    }

    override fun start() {
        if (sideJob != null) return
        controller.startCycle(this, machineScope)
    }

    override fun stop() {
        if (sideJob != null) return

        stopAllActivities()
        controller.stopCycle()
        sideJob = machineScope.launch {
            delay(1 * seconds)
            drainUntilEmpty()
            delay(1 * seconds)
            unlockDoor()
            sideJob = null
        }
    }

    fun togglePower() {
        if (sideJob != null) return

        when {
            running -> stop()
            poweredOn -> controller.powerOff()
            else -> controller.powerOn()
        }
    }

    fun toggleCycleRun() {
        if (sideJob != null) return

        if (controller.cycleRunning) {
            controller.toggleCyclePause()
            if (paused) {
                stopAllActivities()
                if (drum.excessLiquidAmount <= config.doorSafeWaterLevel) {
                    sideJob = machineScope.launch {
                        delay(1 * seconds)
                        unlockDoor()
                        sideJob = null
                    }
                }
            } else {
                lockDoor()
            }
        } else start()
    }

    fun increaseTemperature() = controller.increaseTemperature()
    fun decreaseTemperature() = controller.decreaseTemperature()
    fun increaseSpinSpeed() = controller.increaseSpinSpeed().also { updateCentrifugeSpeed() }
    fun decreaseSpinSpeed() = controller.decreaseSpinSpeed().also { updateCentrifugeSpeed() }

    private fun updateCentrifugeSpeed() {
        if (activeWashCycle?.activePhase is SpinPhase) {
            drumMotor.speedSetting = activeWashCycle!!.selectedSpinSpeedSetting!!
        }
    }

    internal fun lockDoor() {
        doorLocked = true
    }

    internal fun unlockDoor() {
        doorLocked = false
    }

    protected open fun stopAllActivities() {
        dispenser.haltMainDetergent()
        dispenser.haltMainSoftener()
        drumMotor.stop()
        pump.stop()
    }

    internal open suspend fun fillThroughDetergent(amount: Measure<Volume>) {
        dispenser.dispenseMainDetergent()
        trackLiquidUntil { it >= amount }
        dispenser.haltMainDetergent()
    }

    internal open suspend fun fillThroughSoftener(amount: Measure<Volume>) {
        dispenser.dispenseMainSoftener()
        trackLiquidUntil { it >= amount }
        dispenser.haltMainSoftener()
    }

    internal open suspend fun drainUntilEmpty() {
        if ((drum.excessLiquidAmount `in` liters) < 1) return
        startDrain()
        trackLiquidUntil { it.isNegligible() }
        stopDrain()
    }

    internal open suspend fun startDrain() {
        pump.start()
    }

    internal open suspend fun stopDrain() {
        pump.stop()
    }

    internal open suspend fun wash(params: WashParams) {
        with(params) {
            val setTemperature = temperature
            if (setTemperature != null) {
                thermostat.triggerSetting = setTemperature
            }

            val cycleCount = (duration / (spinPeriod + restPeriod)).roundToInt()
            repeat(cycleCount) { i ->
                val direction = if (i % 2 == 0) SpinDirection.Positive else SpinDirection.Negative
                val currentTemperature = drum.excessLiquid.temperature
                if (setTemperature != null && currentTemperature != null) {
                    thermostat.check(currentTemperature)
                } else if (drum.heater.running) {
                    drum.heater.stop()
                }

                spin(direction, spinSpeed, spinPeriod)
                delay(restPeriod / SpeedModifier)
            }

            if (drum.heater.running) drum.heater.stop()
        }
    }

    internal open suspend fun centrifuge(params: CentrifugeParams) {
        with(params) {
            pump.start()
            spin(SpinDirection.Positive, spinSpeed, duration)
            pump.stop()
        }
    }

    private suspend fun spin(direction: SpinDirection, speed: Measure<Spin>, duration: Measure<Time>) {
        drumMotor.speedSetting = speed
        drumMotor.spinDirection = direction
        drumMotor.start()
        delay(duration / SpeedModifier)
        drumMotor.stop()
    }

    protected suspend fun trackLiquidUntil(
        measureRate: Measure<Frequency> = config.waterMeasureRate,
        condition: (liquidVolume: Measure<Volume>) -> Boolean
    ) {
        repeatPeriodically(measureRate) {
            if (condition(drum.excessLiquidAmount)) return
        }
    }
}