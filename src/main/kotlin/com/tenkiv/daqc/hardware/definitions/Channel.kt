package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.device.Device
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.newSingleThreadContext

/**
 * Created by tenkiv on 3/18/17.
 */
interface Channel<T: DaqcValue> {

    val device: Device

    val hardwareType: HardwareType

    val hardwareNumber: Int

    fun activate()

    fun deactivate()

}