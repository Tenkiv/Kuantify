/*
 * Copyright 2020 Tenkiv, Inc.
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

package org.tenkiv.kuantify.fs.gate.acquire

import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.hardware.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.kuantify.trackable.*
import physikal.*
import kotlin.reflect.*

public sealed class FSRemoteInput<T : DaqcValue, out D : FSRemoteDevice>(device: D, uid: String) :
    FSRemoteAcquireGate<T, D>(device, uid), Input<T> {

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    public override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    internal val _isTransceiving = Updatable(false)
    public override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)
        routing.addToThisPath {
            bind<Boolean>(RC.IS_TRANSCEIVING) {
                receive {
                    val value = Serialization.json.parse(Boolean.serializer(), it)
                    _isTransceiving.value = value
                }
            }
        }
    }

}

public abstract class FSRemoteQuantityInput<QT : Quantity<QT>, out D : FSRemoteDevice>(
    device: D,
    uid: String
) : FSRemoteInput<DaqcQuantity<QT>, D>(device, uid), QuantityInput<QT> {

    public abstract val quantityType: KClass<QT>

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)
        routing.addToThisPath {
            bind<QuantityMeasurement<QT>>(RC.VALUE) {
                receive {
                    val measurement = Serialization.json.parse(Measurement.quantitySerializer<QT>(), it)
                    _updateBroadcaster.offer(measurement)
                }
            }
        }
    }

}

public abstract class FSRemoteBinaryStateInput<out D : FSRemoteDevice>(device: D, uid: String) :
    FSRemoteInput<BinaryState, D>(device, uid), BinaryStateInput {

    public override fun sideRouting(routing: SideNetworkRouting<String>) {
        super.sideRouting(routing)
        routing.addToThisPath {
            bind<BinaryStateMeasurement>(RC.VALUE) {
                receive {
                    val measurement = Serialization.json.parse(Measurement.binaryStateSerializer(), it)
                    _updateBroadcaster.offer(measurement)
                }
            }
        }
    }

}
