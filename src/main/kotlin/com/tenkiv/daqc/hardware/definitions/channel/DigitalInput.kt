package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.Channel
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

abstract class DigitalInput :
        Input<ValueInstant<DaqcValue>>,
        Channel<DaqcValue> {

    val transitionFrequencyBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<Frequency>>>()

    val pwmBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<Dimensionless>>>()

    val currentStateBroadcastChannel = ConflatedBroadcastChannel<ValueInstant<BinaryState>>()

    abstract fun activateForTransitionFrequency()

    abstract fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>)

    open fun activateForCurrentState() = activate()
}