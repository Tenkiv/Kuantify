/*
 * Copyright 2018 Tenkiv, Inc.
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

package org.tenkiv.kuantify.hardware.inputs

import kotlinx.coroutines.CoroutineScope
import org.tenkiv.kuantify.data.DaqcQuantity
import org.tenkiv.kuantify.data.toDaqc
import org.tenkiv.kuantify.gate.acquire.input.QuantityInput
import org.tenkiv.kuantify.hardware.definitions.channel.DigitalInput
import org.tenkiv.physikal.core.*
import javax.measure.quantity.Dimensionless
import javax.measure.quantity.Frequency

/**
 * A simple simple implementation of a binary pulse width modulation sensor
 *
 * @param digitalInput The [DigitalInput] that is being read from.
 */
class SimplePwmSensor internal constructor(val digitalInput: DigitalInput) :
    QuantityInput<Dimensionless>, CoroutineScope by digitalInput {

    /**
     * The [Frequency] over which to average the samples.
     */
    @Volatile
    var avgFrequency: DaqcQuantity<Frequency> = 1.hertz.toDaqc()

    override val broadcastChannel get() = digitalInput.pwmBroadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isTransceiving get() = digitalInput.isTransceivingPwm

    override fun startSampling() = digitalInput.startSamplingPwm(avgFrequency)

    override fun stopTransceiving() = digitalInput.stopTransceiving()

    override val updateRate get() = digitalInput.updateRate
}