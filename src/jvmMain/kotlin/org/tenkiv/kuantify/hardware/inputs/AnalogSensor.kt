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

package org.tenkiv.kuantify.hardware.inputs

import mu.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.acquire.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.lib.physikal.*
import physikal.*

private val logger = KotlinLogging.logger {}

/**
 * Abstract class for single channel analog sensorMap.
 *
 * @param analogInput The analog input.
 * @param maxVoltage The maximum [Voltage] for the sensor.
 * @param acceptableError The maximum acceptable error for the sensor in [Voltage].
 */
public abstract class AnalogSensor<QT : Quantity<QT>>(
    public val analogInput: AnalogInput,
    maxVoltage: Quantity<Voltage>,
    acceptableError: Quantity<Voltage>
) : ProcessedAcquireChannel<DaqcQuantity<QT>, DaqcQuantity<Voltage>>(), QuantityInput<QT> {
    public override val updateRate: UpdateRate
        get() = analogInput.updateRate

    protected final override val parentGate: AcquireChannel<DaqcQuantity<Voltage>>
        get() = analogInput

    init {
        analogInput.maxVoltage.set(maxVoltage)
        analogInput.maxAcceptableError.set(acceptableError)

        initCoroutines()
    }

    protected override suspend fun transformationFailure(failure: FailedMeasurement) {
        logger.warn(::transformFailureMsg)
        super.transformationFailure(failure)
    }

    private fun transformFailureMsg() = """Analog sensor based on analog input $analogInput failed to transform input.
        |The value of this input will not be updated.""".trimToSingleLine()

}