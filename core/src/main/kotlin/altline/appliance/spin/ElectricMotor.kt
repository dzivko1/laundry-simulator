package altline.appliance.spin

import altline.appliance.electricity.BasicElectricalDevice
import altline.appliance.measure.Power
import altline.appliance.measure.Spin
import altline.appliance.measure.Spin.Companion.rpm
import io.nacular.measured.units.*

class ElectricMotor(
    power: Measure<Power>,
    val maxSpeed: Measure<Spin>
) : BasicElectricalDevice(power) {

    var connectedLoad: Spinnable? = null

    var spinDirection: SpinDirection = SpinDirection.Positive

    var speedSetting: Measure<Spin> = maxSpeed
        set(value) {
            require(value in (0.0 * rpm)..maxSpeed)
            field = value
        }

    var currentSpeed: Measure<Spin> = 0 * rpm
        private set

    override fun onStop() {
        currentSpeed = 0 * rpm
    }

    override fun operate() {
        val requiredEnergy = (power * timeFactor) * (speedSetting / maxSpeed)
        val availableEnergy = pullEnergy(requiredEnergy, timeFactor)?.amount
        if (availableEnergy != null) {
            val energyRatio = availableEnergy / requiredEnergy
            currentSpeed = speedSetting * energyRatio
            connectedLoad?.spin(spinDirection, currentSpeed, timeFactor)
        }
    }
}