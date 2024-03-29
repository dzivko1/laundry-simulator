package altline.appliance.transit

import altline.appliance.common.RefreshPeriod
import altline.appliance.common.TimeFactor
import io.nacular.measured.units.*

interface Flowable<QuantityType : Units> {
    val amount: Measure<QuantityType>
}

interface MutableFlowable<QuantityType : Units> : Flowable<QuantityType> {
    fun add(other: MutableFlowable<QuantityType>)
    fun extract(amount: Measure<QuantityType>): MutableFlowable<QuantityType>
    fun extractAll(): MutableFlowable<QuantityType>
}

interface FlowSource<QuantityType : Units, FlowableType : Flowable<QuantityType>> {
    val maxOutputFlowRate: Measure<UnitsRatio<QuantityType, Time>>
    val realOutputFlowRate: Measure<UnitsRatio<QuantityType, Time>>

    val outputCount: Int
    val outputs: Array<Port<QuantityType, FlowableType>>

    val tickPeriod get() = RefreshPeriod
    val timeFactor get() = TimeFactor

    fun disconnectOutputs() {
        outputs.forEach { it.disconnect() }
    }

    fun pullFlow(amount: Measure<QuantityType>, timeFrame: Measure<Time>, flowId: Long): FlowableType?

    class Port<QuantityType : Units, FlowableType : Flowable<QuantityType>>(
        val owner: FlowSource<QuantityType, FlowableType>
    ) {
        var connectedPort: FlowDrain.Port<QuantityType, FlowableType>? = null
            private set

        val isConnected get() = connectedPort != null

        infix fun connectTo(receiverPort: FlowDrain.Port<QuantityType, FlowableType>) {
            if (connectedPort == receiverPort) return
            if (connectedPort != null) disconnect()
            connectedPort = receiverPort
            receiverPort connectTo this
        }

        fun disconnect() {
            connectedPort?.let {
                connectedPort = null
                it.disconnect()
            }
        }
    }
}

interface FlowDrain<QuantityType : Units, FlowableType : Flowable<QuantityType>> {
    val maxInputFlowRate: Measure<UnitsRatio<QuantityType, Time>>
    val realInputFlowRate: Measure<UnitsRatio<QuantityType, Time>>

    val inputCount: Int
    val inputs: Array<Port<QuantityType, FlowableType>>

    val tickPeriod get() = RefreshPeriod
    val timeFactor get() = TimeFactor

    fun disconnectInputs() {
        inputs.forEach { it.disconnect() }
    }

    fun pushFlow(flowable: FlowableType, timeFrame: Measure<Time>, flowId: Long): Measure<QuantityType>

    class Port<QuantityType : Units, FlowableType : Flowable<QuantityType>>(
        val owner: FlowDrain<QuantityType, FlowableType>
    ) {
        var connectedPort: FlowSource.Port<QuantityType, FlowableType>? = null
            private set

        val isConnected get() = connectedPort != null

        infix fun connectTo(senderPort: FlowSource.Port<QuantityType, FlowableType>) {
            if (connectedPort == senderPort) return
            if (connectedPort != null) disconnect()
            connectedPort = senderPort
            senderPort connectTo this
        }

        fun disconnect() {
            connectedPort?.let {
                connectedPort = null
                it.disconnect()
            }
        }
    }
}

interface Conduit<QuantityType : Units, FlowableType : Flowable<QuantityType>>
    : FlowSource<QuantityType, FlowableType>, FlowDrain<QuantityType, FlowableType> {

    override val tickPeriod get() = RefreshPeriod
    override val timeFactor get() = TimeFactor
}