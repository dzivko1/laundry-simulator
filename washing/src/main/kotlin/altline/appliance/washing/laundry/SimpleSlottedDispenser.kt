package altline.appliance.washing.laundry

import altline.appliance.measure.Volume
import altline.appliance.substance.MutableSubstance
import altline.appliance.substance.transit.BasicSubstanceConduit
import io.nacular.measured.units.*

class SimpleSlottedDispenser(
    mainDetergentCapacity: Measure<Volume>,
    mainSoftenerCapacity: Measure<Volume>,
    config: LaundryWasherConfig
) : SlottedDispenser(config) {

    override val tray = Tray(mainDetergentCapacity, mainSoftenerCapacity, config)
    override val solventInputJunction = BasicSubstanceConduit(config.intakeFlowRate, 1, 2)
    override val solventOutputJunction = BasicSubstanceConduit(config.intakeFlowRate, 2, 1)

    private val mainDetergentChannel = Channel(0)
    private val mainSoftenerChannel = Channel(1)

    override val isDispensingMainDetergent: Boolean
        get() = mainDetergentChannel.isDispensing

    override val isDispensingMainSoftener: Boolean
        get() = mainSoftenerChannel.isDispensing

    override fun refreshTrayConnections() {
        mainDetergentChannel.refreshTrayConnections()
        mainSoftenerChannel.refreshTrayConnections()
    }

    override fun dispenseMainDetergent() {
        mainDetergentChannel.dispense()
    }

    override fun haltMainDetergent() {
        mainDetergentChannel.halt()
    }

    override fun dispenseMainSoftener() {
        mainSoftenerChannel.dispense()
    }

    override fun haltMainSoftener() {
        mainSoftenerChannel.halt()
    }

    class Tray(
        mainDetergentCapacity: Measure<Volume>,
        mainSoftenerCapacity: Measure<Volume>,
        config: LaundryWasherConfig
    ) : SlottedDispenser.Tray {

        val mainDetergentSlot = SlottedDispenser.Tray.Slot(mainDetergentCapacity, config.intakeFlowRate)
        val mainSoftenerSlot = SlottedDispenser.Tray.Slot(mainSoftenerCapacity, config.intakeFlowRate)

        override fun getSlot(index: Int): SlottedDispenser.Tray.Slot {
            return when (index) {
                0 -> mainDetergentSlot
                1 -> mainSoftenerSlot
                else -> throw IndexOutOfBoundsException(index)
            }
        }

        override fun empty(): MutableSubstance {
            return MutableSubstance().apply {
                add(mainDetergentSlot.empty())
                add(mainSoftenerSlot.empty())
            }
        }
    }
}