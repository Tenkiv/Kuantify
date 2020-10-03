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

package kuantify.fs.gate.acquire

import kuantify.data.*
import kuantify.fs.networking.*
import kuantify.gate.acquire.input.*
import kuantify.lib.*
import kuantify.networking.configuration.*
import physikal.*

public abstract class LocalInput<T : DaqcValue>(uid: String) : LocalAcquireChannel<T>(uid), Input<T>

public abstract class LocalQuantityInput<QT : Quantity<QT>>(uid: String) : LocalInput<DaqcQuantity<QT>>(uid),
    QuantityInput<QT> {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS<QuantityMeasurement<QT>>(QuantityMeasurement.quantitySerializer(), RC.VALUE) {
                send(source = openSubscription())
            }
        }
    }

}

public abstract class LocalBinaryStateInput(uid: String) : LocalInput<BinaryState>(uid), BinaryStateInput {

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindFS(BinaryStateMeasurement.binaryStateSerializer(), RC.VALUE) {
                send(source = openSubscription())
            }
        }
    }

}