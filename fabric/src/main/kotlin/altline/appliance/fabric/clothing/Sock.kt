package altline.appliance.fabric.clothing

import altline.appliance.measure.Volume
import io.nacular.measured.units.*

class Sock(
    size: Size,
    volume: Measure<Volume>
) : Clothing(size, volume)