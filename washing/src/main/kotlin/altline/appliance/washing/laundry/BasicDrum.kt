package altline.appliance.washing.laundry

import altline.appliance.common.Body
import altline.appliance.common.volume
import altline.appliance.electricity.ElectricHeater
import altline.appliance.measure.Spin
import altline.appliance.measure.Temperature.Companion.celsius
import altline.appliance.measure.Volume
import altline.appliance.measure.Volume.Companion.liters
import altline.appliance.substance.MutableSubstance
import altline.appliance.substance.Soakable
import altline.appliance.substance.fresheningPotential
import altline.appliance.substance.transit.Reservoir
import altline.appliance.washing.cleaningPower
import io.nacular.measured.units.*
import io.nacular.measured.units.Time.Companion.seconds

class BasicDrum(
    capacity: Measure<Volume>,
    override val heater: ElectricHeater,
    private val config: LaundryWasherConfig
) : Drum,
    Reservoir(
        capacity,
        0.0 * liters,
        false,
        config.intakeFlowRate,
        config.outputFlowRate
    ) {

    override val inputPort = inputs[0]
    override val outputPort = outputs[0]

    private val _laundry = mutableListOf<Body>()
    val laundry = _laundry as List<Body>

    override val excessLiquidAmount: Measure<Volume>
        get() = storedSubstanceAmount

    init {
        heater.heatedSubstance = storedSubstance
    }

    override fun load(vararg items: Body) {
        if (laundry.volume + items.volume <= capacity) {
            _laundry += items
        }
    }

    override fun unload(vararg items: Body) {
        _laundry -= items.toSet()
    }

    override fun unloadAll(): List<Body> {
        return ArrayList(laundry).also { _laundry.clear() }
    }

    override fun pushFlow(flowable: MutableSubstance, timeFrame: Measure<Time>, flowId: Long): Measure<Volume> {
        return super.pushFlow(flowable, timeFrame, flowId).also {
            soakLaundry()
        }
    }

    private fun soakLaundry() {
        laundry.forEach { piece ->
            if (piece is Soakable) {
                piece.soak(storedSubstance)
            }
        }
    }

    override fun spin(speed: Measure<Spin>, duration: Measure<Time>) {
        for (piece in laundry) {
            wash(piece, speed, duration `in` seconds)
        }
    }

    private fun wash(body: Body, spinSpeed: Measure<Spin>, seconds: Double) {
        if (spinSpeed > config.centrifugeThreshold) return

        val spinEffectiveness = spinSpeed / config.centrifugeThreshold

        if (body is Soakable) {
            // resoak
            val resoakAmount = minOf(body.soakedSubstance.amount, excessLiquidAmount) *
                    config.nominalResoakFactor * spinEffectiveness
            body.resoakWith(storedSubstance, resoakAmount)

            // freshen
            val diff = body.soakedSubstance.fresheningPotential - body.freshness
            val step = diff / 10 * spinEffectiveness * seconds
            body.freshness += step
        }

        // clean
        val effectiveCleaningPower = calcCleaningPower(body) * spinEffectiveness
        val stainAmountToClear = body.stainSubstance.amount * effectiveCleaningPower * seconds
        val clearedStain = body.clearStain(stainAmountToClear)
        storedSubstance.add(clearedStain)
    }

    private fun calcCleaningPower(body: Body): Double {
        return if (body is Soakable) {
            val soakRatio = body.soakedSubstance.amount / body.volume
            val soakCoefficient = ((soakRatio - config.lowerSoakRatio) / config.upperSoakRatio - config.lowerSoakRatio)
                .coerceIn(0.0, 1.0)
            val temperatureCoefficient = (body.soakedSubstance.temperature / (100 * celsius))
                .coerceIn(0.0, 1.0)
            body.soakedSubstance.cleaningPower * soakCoefficient * temperatureCoefficient

        } else {
            val temperatureCoefficient = (storedSubstance.temperature / (100 * celsius))
                .coerceIn(0.0, 1.0)
            storedSubstance.cleaningPower * temperatureCoefficient
        }
    }
}