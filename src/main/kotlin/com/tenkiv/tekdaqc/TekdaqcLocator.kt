package com.tenkiv.tekdaqc

import com.tenkiv.daqc.networking.DeviceLocator
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.locator.Locator
import com.tenkiv.tekdaqc.locator.OnTekdaqcDiscovered
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import java.util.concurrent.CopyOnWriteArrayList

class TekdaqcLocator : OnTekdaqcDiscovered, DeviceLocator<List<TekdaqcBoard>>() {

    override val activeDevices: List<TekdaqcBoard> = CopyOnWriteArrayList<TekdaqcBoard>()

    override val broadcastChannel = ConflatedBroadcastChannel(activeDevices)

    init {
        Locator.instance.addLocatorListener(this)
    }

    override fun onTekdaqcResponse(tekdaqc: ATekdaqc) {}

    override fun onTekdaqcNoLongerLocated(tekdaqc: ATekdaqc) {
        (activeDevices as? MutableList)?.removeIf { it.tekdaqc.serialNumber == tekdaqc.serialNumber }
        broadcastChannel.offer(activeDevices)
    }

    override fun onTekdaqcFirstLocated(tekdaqc: ATekdaqc) {
        val board = TekdaqcBoard(tekdaqc)
        (activeDevices as? MutableList)?.add(board)
        broadcastChannel.offer(activeDevices)
    }

    override fun search() {
        Locator.instance.searchForTekdaqcs()
    }

    override fun stop() {
        Locator.instance.cancelLocator()
    }

}