import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.Sensor
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import tec.uom.se.unit.Units
import java.util.*

/**
 * Created by tenkiv on 5/22/17.
 */
class PredicatbleSensor : Sensor<DaqcValue.Boolean>(emptyList<Input<DaqcValue.Boolean>>()) {

    override val onDataReceived: suspend (Updatable<DaqcValue.Boolean>) -> Unit
        //Never gets data. This is a fake sensor.
        get() = { println("What on Earth happened here?") }


    var iteration = 0

    var sendingOrder = arrayListOf(
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(true),
                        DaqcValue.Boolean(false),
                        DaqcValue.Boolean(false))

    init {
        val timer = Timer(false)

        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                value = sendingOrder[iteration]
                iteration++
                if(iteration == sendingOrder.size){ this.cancel() }
            }
        },100,100)
    }

}