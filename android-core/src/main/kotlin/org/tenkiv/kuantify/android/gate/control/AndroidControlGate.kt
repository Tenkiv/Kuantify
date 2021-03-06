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

package org.tenkiv.kuantify.android.gate.control

import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.android.gate.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.control.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.gate.control.output.*
import javax.measure.*
import kotlin.reflect.*

public typealias AndroidQuantityOutput<Q> = AndroidOutput<DaqcQuantity<Q>>

public interface AndroidControlGate<T : DaqcData> : AndroidDaqcGate<T>, ControlGate<T>

public interface AndroidOutput<T : DaqcValue> : AndroidDaqcGate<T>, Output<T>

public class AndroidRemoteQuantityOutput<Q : Quantity<Q>>(
    device: RemoteAndroidDevice,
    uid: String,
    public override val quantityType: KClass<Q>
) : FSRemoteQuantityOutput<Q, RemoteAndroidDevice>(device, uid), AndroidQuantityOutput<Q>

public class AndroidRemoteBinaryStateOutput(
    device: RemoteAndroidDevice,
    uid: String
) : FSRemoteBinaryStateOutput<RemoteAndroidDevice>(device, uid), AndroidOutput<BinaryState>