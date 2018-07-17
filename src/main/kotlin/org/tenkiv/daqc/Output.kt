package org.tenkiv.daqc

import org.tenkiv.coral.ValueInstant
import org.tenkiv.physikal.core.PhysicalUnit
import org.tenkiv.physikal.core.invoke
import tec.uom.se.ComparableQuantity
import javax.measure.Quantity

//typealias QuantityOutput<Q> = Output<DaqcQuantity<Q>>

fun <Q : Quantity<Q>> QuantityOutput<Q>.setOutput(setting: ComparableQuantity<Q>) = setOutput(setting.toDaqc())

interface Output<T : DaqcValue> : Updatable<ValueInstant<T>> {

    val isActive: Boolean

    /**
     * @throws Throwable if something prevents this output from being set.
     */
    fun setOutput(setting: T)

    fun deactivate()

}

interface QuantityOutput<Q : Quantity<Q>> : Output<DaqcQuantity<Q>> {

    private val systemUnit: PhysicalUnit<Q> get() = broadcastChannel.value.value.unit.systemUnit

    fun setOutputInSystemUnit(setting: Double) = setOutput(setting(systemUnit))

}

interface RangedOutput<T> : Output<T> where T : DaqcValue, T : Comparable<T> {

    val possibleOutputRange: ClosedRange<T>

}

interface BinaryStateOutput : RangedOutput<BinaryState> {

    override val possibleOutputRange get() = BinaryState.range

}

// Kotlin compiler is getting confused about generics star projections if RangedOutput (or a typealias) is used directly
// TODO: look into changing this to a typealias if generics compiler issue is fixed.
interface RangedQuantityOutput<Q : Quantity<Q>> : RangedOutput<DaqcQuantity<Q>>

class RangedQuantityOutputBox<Q : Quantity<Q>>(
    output: QuantityOutput<Q>,
    private val getPossibleOutputRange: () -> ClosedRange<DaqcQuantity<Q>>
) : RangedQuantityOutput<Q>, QuantityOutput<Q> by output {

    override val possibleOutputRange get() = getPossibleOutputRange()

}

fun <Q : Quantity<Q>> QuantityOutput<Q>.toNewRangedOutput(possibleOutputRange: () -> ClosedRange<DaqcQuantity<Q>>) =
    RangedQuantityOutputBox(this, possibleOutputRange)