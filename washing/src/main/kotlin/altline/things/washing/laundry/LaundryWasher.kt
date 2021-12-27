package altline.things.washing.laundry

import altline.things.common.Body
import altline.things.electricity.ElectricalConduit
import altline.things.electricity.ElectricalDevice
import altline.things.measure.Volume
import altline.things.washing.Washer
import io.nacular.measured.units.Measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

abstract class LaundryWasher(

) : Washer, ElectricalDevice {

    private val machineScope = CoroutineScope(Dispatchers.Default)

    protected abstract val washCyclesImpl: List<WashCycle>
    val washCycles: List<String> by lazy { washCyclesImpl.map { it.name } }

    protected val selectedWashCycleImpl: WashCycle
        get() = washCyclesImpl[selectedWashCycle]

    var selectedWashCycle: Int = 0
        private set

    fun selectWashCycle(id: Int) {
        TODO("Not yet implemented")
    }

    override fun connectPowerSource(source: ElectricalConduit) {
        TODO("Not yet implemented")
    }

    override fun disconnectPowerSource() {
        TODO("Not yet implemented")
    }

    override fun load(item: Body) {
        TODO("Not yet implemented")
    }

    override fun load(items: Collection<Body>) {
        TODO("Not yet implemented")
    }

    override fun unload(item: Body) {
        TODO("Not yet implemented")
    }

    override fun unload(items: Collection<Body>) {
        TODO("Not yet implemented")
    }

    override fun unload(): List<Body> {
        TODO("Not yet implemented")
    }

    override fun start() = selectedWashCycleImpl.start(machineScope)
    override fun stop() = selectedWashCycleImpl.stop()

    internal abstract suspend fun fillThroughDetergent(amount: Measure<Volume>)
    internal abstract suspend fun fillThroughSoftener(amount: Measure<Volume>)
    internal abstract suspend fun drain()
    internal abstract suspend fun wash(params: WashParams)
    internal abstract suspend fun spin(params: CentrifugeParams)

    interface WashCycle {
        val name: String
        val machine: LaundryWasher
        fun start(coroutineScope: CoroutineScope)
        fun stop()
    }
}