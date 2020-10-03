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

package kuantify.fs.gate.control

import kotlinx.coroutines.channels.*
import kuantify.data.*
import kuantify.fs.gate.*
import kuantify.fs.networking.*
import kuantify.gate.control.*
import kuantify.gate.control.output.*
import kuantify.lib.*
import kuantify.networking.configuration.*
import physikal.*

public abstract class LocalOutput<T : DaqcValue>(uid: String) : LocalDaqcGate(uid), Output<T> {
    internal val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<T>>()
    public override val valueOrNull: ValueInstant<T>?
        get() = broadcastChannel.valueOrNull

    public override fun openSubscription(): ReceiveChannel<ValueInstant<T>> =
        broadcastChannel.openSubscription()
}

public abstract class LocalQuantityOutput<QT : Quantity<QT>>(uid: String) : LocalOutput<DaqcQuantity<QT>>(uid),
    QuantityOutput<QT> {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS<QuantityMeasurement<QT>>(QuantityMeasurement.quantitySerializer(), RC.VALUE) {
                send(source = openSubscription())
            }
            bindFS<Quantity<QT>>(Quantity.serializer(), RC.CONTROL_SETTING) {
                receive { setting ->
                    setOutput(setting)
                }
            }
        }
    }

}

public abstract class LocalBinaryStateOutput(uid: String) : LocalOutput<BinaryState>(uid), BinaryStateOutput {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(BinaryStateMeasurement.binaryStateSerializer(), RC.VALUE) {
                send(source = openSubscription())
            }
            bindFS(BinaryState.serializer(), RC.CONTROL_SETTING) {
                receive { setting ->
                    setOutput(setting)
                }
            }
        }
    }

}