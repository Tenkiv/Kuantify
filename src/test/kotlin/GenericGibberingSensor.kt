import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.Sensor
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import com.tenkiv.daqc.hardware.definitions.channel.Input
import com.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.nexus.data.MILLIVOLT
import org.tenkiv.nexus.data.VOLT
import tec.uom.se.unit.MetricPrefix
import tec.uom.se.unit.Units
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 4/13/17.
 */
class GenericGibberingSensor : Sensor<DaqcValue>(emptyList<Input<DaqcValue>>()) {

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                value = DaqcValue.Quantity.of(random.nextInt(5), Units.VOLT)
            }
        },100,100)
    }

    override val onDataReceived: UpdatableListener<DaqcValue> = object : UpdatableListener<DaqcValue>{
        override fun onUpdate(updatedObject: Updatable<DaqcValue>) {
            //Never Called
        }
    }

    fun cancel(){
        timer.cancel()
    }
}

class AnalogGibberingSensor: Sensor<DaqcValue.Quantity<ElectricPotential>>(emptyList<Input<DaqcValue>>()) {

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                value = DaqcValue.Quantity.of(random.nextInt(5000), MetricPrefix.MILLI(Units.VOLT))
                println("Value $value")
            }
        },100,100)
    }

    fun cancel(){
        timer.cancel()
    }

    override val onDataReceived: UpdatableListener<DaqcValue> = object : UpdatableListener<DaqcValue>{
        override fun onUpdate(updatedObject: Updatable<DaqcValue>) {
            //Never Called
        }
    }
}