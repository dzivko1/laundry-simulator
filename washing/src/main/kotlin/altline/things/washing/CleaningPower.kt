package altline.things.washing

import altline.things.measure.Volume.Companion.liters
import altline.things.measure.Volume.Companion.milliliters
import altline.things.substances.CommonSubstanceTypes
import altline.things.substances.Substance
import altline.things.substances.SubstanceType
import altline.things.substances.calcNominal
import altline.things.washing.laundry.CommonFabricSofteners
import io.nacular.measured.units.div
import io.nacular.measured.units.times

val Substance.cleaningPower: Double
    get() {
        var powerSum = 0.0
        parts.forEach { part ->
            val ratio = part.amount / totalAmount
            val effectivePower = part.type.cleaningPower * ratio
            powerSum += effectivePower
        }
        return powerSum
    }

val SubstanceType.cleaningPower: Double
    get() {
        return when (this) {
            is DetergentType -> cleaningPower

            CommonFabricSofteners.USELESS_SOFTENER -> 0.0
            CommonFabricSofteners.BARELY_SOFTENER -> convertDiluted(0.00005)
            CommonFabricSofteners.WEAK_SOFTENER -> convertDiluted(0.0001)
            CommonFabricSofteners.MILD_SOFTENER -> convertDiluted(0.00015)
            CommonFabricSofteners.BASIC_SOFTENER -> convertDiluted(0.0002)
            CommonFabricSofteners.STRONG_SOFTENER -> convertDiluted(0.00025)
            CommonFabricSofteners.ULTIMATE_SOFTENER -> convertDiluted(0.0003)

            CommonSubstanceTypes.WATER -> 0.0001
            else -> 0.0
        }
    }

private fun convertDiluted(desiredValue: Double) = calcNominal(
    desiredValue,
    solventValue = CommonSubstanceTypes.WATER.cleaningPower,
    solventAmount = 15.0 * liters,
    soluteAmount = 50.0 * milliliters
)