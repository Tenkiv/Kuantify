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

package kuantify.gate.acquire.input

import org.tenkiv.coral.*
import kuantify.data.*
import kuantify.gate.*
import kuantify.gate.acquire.*
import physikal.*

public typealias QuantityInput<QT> = Input<DaqcQuantity<QT>>

/**
 * Interface defining classes which act as inputs and measure or gather data.
 *
 * @param T The type of data given by this Input.
 */
public interface Input<out T : DaqcValue> : AcquireChannel<T>, IOStrand<T>

/**
 * An Input whose type is both a [DaqcValue] and [Comparable] allowing it to be used in the default learning module
 * classes
 */
public interface RangedInput<T> : Input<T>, RangedIOStrand<T> where T : DaqcValue, T : Comparable<T>

/**
 * A [RangedInput] which supports the [BinaryState] type.
 */
public interface BinaryStateInput : RangedInput<BinaryState> {

    public override val valueRange: ClosedRange<BinaryState> get() = BinaryState.range

}

public interface RangedQuantityInput<QT : Quantity<QT>> : RangedInput<DaqcQuantity<QT>> {
    public override val valueRange: ClosedFloatingPointRange<DaqcQuantity<QT>>
}

//TODO: Consider handling out of range updates
private class RangedQuantityInputBox<QT : Quantity<QT>>(
    private val input: QuantityInput<QT>,
    override val valueRange: ClosedFloatingPointRange<DaqcQuantity<QT>>
) : RangedQuantityInput<QT>, QuantityInput<QT> by input {

    override val daqcDataSize: UInt32
        get() = input.daqcDataSize

}

public fun <QT : Quantity<QT>> QuantityInput<QT>.toNewRangedInput(
    valueRange: ClosedFloatingPointRange<DaqcQuantity<QT>>
): RangedQuantityInput<QT> = RangedQuantityInputBox(this, valueRange)