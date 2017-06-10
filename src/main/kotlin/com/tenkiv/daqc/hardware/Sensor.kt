package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.*
import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.channel.Input
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch

/**
 * Created by tenkiv on 4/17/17.
 */
abstract class Sensor<O: DaqcValue>(inputs: Collection<Input<O>>): Input<O>, Updatable<O>, UpdatableListener<O> {

    init {
        launch(CommonPool){
            inputs.forEach { input -> input.broadcastChannel.consumeEach{ value -> onUpdate(input,value)} }
        }
    }

    override val broadcastChannel: ConflatedBroadcastChannel<O> = ConflatedBroadcastChannel()

    open fun addTrigger(condition: (O) -> Boolean, function: () -> Unit): Trigger<O>
        { return Trigger(TriggerCondition(this, condition) , triggerFunction = function) }

}