/*
 * Copyright 2019 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.fs.gate.control

import kotlinx.coroutines.channels.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import mu.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import tec.units.indriya.*
import javax.measure.*
import kotlin.reflect.*

private val logger = KotlinLogging.logger {}

public sealed class FSRemoteOutput<T : DaqcValue, D : FSRemoteDevice>(device: D, uid: String) :
    FSRemoteControlGate<T, D>(device, uid), Output<T> {

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    public final override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    internal val settingChannel = Channel<T>(Channel.UNLIMITED)

    internal val _isTransceiving = Updatable(false)
    public final override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    public override fun setOutput(setting: T): SettingViability {
        val connected = command { settingChannel.offer(setting) }
        return if (connected) SettingViability.Viable else SettingViability.Unviable(ConnectionException(this))
    }

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<Boolean>(RC.IS_TRANSCEIVING) {
                receive {
                    val value = Json.parse(BooleanSerializer, it)
                    _isTransceiving.value = value
                }
            }
        }
    }
}

public abstract class FSRemoteQuantityOutput<Q : Quantity<Q>, D : FSRemoteDevice>(
    device: D,
    uid: String
) : FSRemoteOutput<DaqcQuantity<Q>, D>(device, uid), QuantityOutput<Q> {

    public abstract val quantityType: KClass<Q>

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<ComparableQuantity<Q>>(RC.VALUE) {
                serializeMessage {
                    Json.stringify(ComparableQuantitySerializer, it)
                }

                setLocalUpdateChannel(settingChannel) withUpdateChannel {
                    send()
                }

                receive {
                    val (value, instant) = Json.parse(ValueInstantSerializer(ComparableQuantitySerializer), it)
                    val setting = value.asType<Q>(quantityType.java).toDaqc()

                    _updateBroadcaster.send(setting at instant)
                }
            }
        }
    }
}

public abstract class FSRemoteBinaryStateOutput<D : FSRemoteDevice>(device: D, uid: String) :
    FSRemoteOutput<BinaryState, D>(device, uid), BinaryStateOutput {

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)

        routing.addToThisPath {
            bind<BinaryState>(RC.VALUE) {
                serializeMessage {
                    Json.stringify(BinaryState.serializer(), it)
                }

                setLocalUpdateChannel(settingChannel) withUpdateChannel {
                    send()
                }

                receive {
                    val settingInstant = Json.parse(ValueInstantSerializer(BinaryState.serializer()), it)

                    _updateBroadcaster.send(settingInstant)
                }
            }
        }
    }
}