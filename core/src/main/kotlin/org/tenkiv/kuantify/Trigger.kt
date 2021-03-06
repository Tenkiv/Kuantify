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

package org.tenkiv.kuantify

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.lib.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

public fun <T> CoroutineScope.Trigger(
    triggerOnSimultaneousValues: Boolean = false,
    maxTriggerCount: MaxTriggerCount = MaxTriggerCount.Limited(1),
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    scope = this,
    triggerOnSimultaneousValues = triggerOnSimultaneousValues,
    maxTriggerCount = maxTriggerCount,
    triggerConditions = *triggerConditions,
    triggerFunction = triggerFunction
)

public fun <T> CoroutineScope.Trigger(
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    this,
    false,
    MaxTriggerCount.Limited(1),
    *triggerConditions,
    triggerFunction = triggerFunction
)

public fun <T> CoroutineScope.Trigger(
    maxTriggerCount: MaxTriggerCount,
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    this,
    false,
    maxTriggerCount,
    *triggerConditions,
    triggerFunction = triggerFunction
)

public fun <T> CoroutineScope.Trigger(
    triggerOnSimultaneousValues: Boolean,
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
): Trigger<T> = Trigger(
    this,
    triggerOnSimultaneousValues,
    MaxTriggerCount.Limited(1),
    *triggerConditions,
    triggerFunction = triggerFunction
)

/**
 * Class which acts as a monitor on an Input to execute a command when a certain state is met.
 *
 * @param triggerOnSimultaneousValues If the Trigger should fire only when all values are met at the same time.
 * @param maxTriggerCount The [MaxTriggerCount] for how many times the trigger should fire until it terminates.
 * @param triggerConditions The [TriggerCondition]s which need to be met for a trigger to fire.
 * @param triggerFunction The function to be executed when the trigger fires.
 */
public class Trigger<T> internal constructor(
    scope: CoroutineScope,
    public val triggerOnSimultaneousValues: Boolean = false,
    public val maxTriggerCount: MaxTriggerCount = MaxTriggerCount.Limited(1),
    vararg triggerConditions: TriggerCondition<T>,
    triggerFunction: () -> Unit
) : CoroutineScope {
    private val job = Job(scope.coroutineContext[Job])

    public override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    private val channelList: MutableList<ReceiveChannel<ValueInstant<T>>> = ArrayList()

    /**
     * Stops the [Trigger] and cancels the open channels.
     */
    public fun cancel() {
        if (maxTriggerCount is MaxTriggerCount.Limited && maxTriggerCount.atomicCount.get() > 0) {
            maxTriggerCount.atomicCount.decrementAndGet()

            if (maxTriggerCount.atomicCount.get() <= 0) {
                channelList.forEach { it.cancel() }
            }
        }
        job.cancel()
    }

    init {
        if (!(maxTriggerCount is MaxTriggerCount.Limited && maxTriggerCount.atomicCount.get() == 0)) {
            triggerConditions.forEach {
                channelList.add(it.trackable.updateBroadcaster.consumeAndReturn(this) { update ->
                    val currentVal = update

                    it.lastValue = currentVal

                    if (it.condition(currentVal)) {
                        it.hasBeenReached = true

                        if (triggerOnSimultaneousValues) {
                            cancel()
                            triggerConditions.all {
                                val value = it.lastValue ?: return@all false
                                it.condition(value)
                            }.apply { triggerFunction() }

                        } else {
                            cancel()
                            triggerConditions.all { it.hasBeenReached }.apply { triggerFunction() }
                        }
                    }
                })
            }
        }
    }
}

/**
 * Sealed class to determine the number of times a [Trigger] should fire.
 */
public sealed class MaxTriggerCount {

    /**
     * Class which sets the number of times a [Trigger] can fire.
     */
    public data class Limited(public val totalCount: Int) : MaxTriggerCount() {

        /**
         * The number of charges left in the [Trigger]
         */
        public val remainingCount get() = atomicCount.get()

        internal val atomicCount = AtomicInteger(totalCount)

    }

    /**
     * Class which sets a [Trigger] to fire unlimited times.
     */
    public object Unlimited : MaxTriggerCount()
}

//TODO: Should support IOStrand, not just Input
/**
 * The condition upon which the [Trigger] will fire.
 *
 * @param trackable The [Input] to monitor.
 * @param condition The conditions upon which to execute the [Trigger]'s function.
 */
public data class TriggerCondition<T>(
    public val trackable: Trackable<ValueInstant<T>>,
    public val condition: (ValueInstant<T>) -> Boolean
) {
    public var lastValue: ValueInstant<T>? = null
    public var hasBeenReached: Boolean = false
}