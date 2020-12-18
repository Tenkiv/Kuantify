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

package kuantify.fs.gate

import kotlinx.coroutines.channels.*
import kotlinx.serialization.builtins.*
import kuantify.*
import kuantify.fs.hardware.device.*
import kuantify.fs.networking.*
import kuantify.gate.*
import kuantify.networking.communication.*
import kuantify.networking.configuration.*
import kuantify.trackable.*
import physikal.*
import physikal.types.*

public abstract class LocalDigitalGate(uid: String) : LocalDaqcGate(uid), DigitalGate {
    public override val avgPeriod: UpdatableQuantity<Time> = Updatable {
        modifyConfiguration {
            setValue(it)
        }
    }

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS<Quantity<Time>>(Quantity.serializer(), RC.AVG_PERIOD) {
                send(source = avgPeriod.flow)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    avgPeriod.set(it)
                }
            }
            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING_BIN_STATE) {
                send(source = isTransceivingBinaryState.flow)
            }
            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING_FREQUENCY) {
                send(source = isTransceivingFrequency.flow)
            }
            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING_PWM) {
                send(source = isTransceivingPwm.flow)
            }
        }
    }

}

public abstract class FSRemoteDigitalGate(uid: String, device: FSRemoteDevice) : FSRemoteDaqcGate(uid), DigitalGate {
    private val _avgPeriod = device.RemoteSyncUpdatable<Quantity<Time>> {
        modifyConfiguration {
            setValue(it)
        }
    }
    public override val avgPeriod: UpdatableQuantity<Time> get() = _avgPeriod

    private val _isTransceivingBinaryState = Updatable<Boolean>()
    public override val isTransceivingBinaryState: Trackable<Boolean>
        get() = _isTransceivingBinaryState

    private val _isTransceivingPwm = Updatable<Boolean>()
    public override val isTransceivingPwm: Trackable<Boolean>
        get() = _isTransceivingPwm

    private val _isTransceivingFrequency = Updatable<Boolean>()
    override val isTransceivingFrequency: Trackable<Boolean>
        get() = _isTransceivingFrequency

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS<Quantity<Time>>(Quantity.serializer(), RC.AVG_PERIOD) {
                send(source = _avgPeriod.localSetChannel)
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        _avgPeriod.update(it)
                    }
                }
            }
            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING_BIN_STATE) {
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        _isTransceivingBinaryState.set(it)
                    }
                }
            }
            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING_FREQUENCY) {
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        _isTransceivingFrequency.set(it)
                    }
                }
            }
            bindFS(Boolean.serializer(), RC.IS_TRANSCEIVING_PWM) {
                receive(networkChannelCapacity = Channel.CONFLATED) {
                    modifyConfiguration {
                        _isTransceivingPwm.set(it)
                    }
                }
            }
        }
    }

}