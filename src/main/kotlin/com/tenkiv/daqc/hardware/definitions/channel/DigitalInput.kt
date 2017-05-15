package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Channel
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 3/18/17.
 */
abstract class DigitalInput: Input<DaqcValue.Boolean>, Channel<DaqcValue.Boolean> {

    override val listeners: MutableList<UpdatableListener<DaqcValue.Boolean>> = CopyOnWriteArrayList()

    private var _value: DaqcValue.Boolean? = null

    override var value: DaqcValue.Boolean?
        get() = _value
        set(value) { _value = value; listeners.forEach{ it.onUpdate(this) } }

    abstract val canReadPulseWidthModulation: Boolean

}