package altline.appliance.washing.laundry.washCycle

import altline.appliance.measure.Spin
import io.nacular.measured.units.*

data class SpinParams(
    val duration: Measure<Time>,
    val spinSpeed: Measure<Spin>
)