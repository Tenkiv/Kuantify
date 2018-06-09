package org.tenkiv.daqc.learning

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.yield
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.distribution.UniformDistribution
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.GravesLSTM
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.now
import org.tenkiv.daqc.*
import org.tenkiv.physikal.core.invoke
import tec.uom.se.unit.Units
import java.time.Duration
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.measure.Quantity
import javax.measure.Unit
import kotlin.concurrent.write

/**
 * Copyright 2017 TENKIV, INC.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


class NnpidController<T : DaqcValue, O : Quantity<O>> @PublishedApi internal constructor(
        private val targetInput: Input<T>,
        private val outputUnit: Unit<O>,
        private val output: Output<DaqcQuantity<O>>,
        private val postProcessor: (Input<T>, Array<out Input<*>>, DaqcQuantity<O>) -> DaqcQuantity<O>,
        private vararg val correlatedInputs: Input<DaqcValue>
) : Output<T> {

    private val _broadcastChannel = ConflatedBroadcastChannel<ValueInstant<T>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _broadcastChannel

    override val isActive: Boolean
        get() = _isActivate

    @Volatile
    private var _isActivate = true

    private var listenJob: Job? = null

    private val correlatedNetwork = if (correlatedInputs.isNotEmpty())
        CorrelatedLstmNetwork(*correlatedInputs)
    else
        null

    private var pidEntryLayerSize = 2

    private var error = 0f
    private var previousError = 0f
    private var integral = 0f

    private var kp = .3f
    private var ki = .4f
    private var kd = .3f

    var out = 0.001f

    private var previousTime: Instant = Instant.now()

    private var failureCount = 0

    private val net: MultiLayerNetwork =
            MultiLayerNetwork(NeuralNetConfiguration.Builder().apply {
                iterations(PID_ITERATIONS)
                weightInit(WeightInit.XAVIER)
                learningRate(PID_LEARNING_RATE)
            }.list().backprop(true).apply {
                layer(0, DenseLayer.Builder().apply {
                    nIn(1)
                    nOut(4)
                    activation(Activation.TANH)
                }.build())
                layer(1, GravesLSTM.Builder().apply {
                    nIn(4)
                    nOut(4)
                    activation(Activation.TANH)
                }.build())
                layer(2, RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE).apply {
                    nIn(4)
                    nOut(1)
                    activation(Activation.IDENTITY)
                }.build())
            }.build())

    private fun runJob(desiredValue: T) {
        listenJob = targetInput.openNewCoroutineListener(CommonPool) {

            val pid = runPid(desiredValue, it)

            val recentVal = it.value.toPidFloat()

            correlatedNetwork?.train(desiredValue.toPidFloat(), recentVal)

            val trainArray = Nd4j.create(
                    /*if (correlatedNetwork != null) {
                        val correlatedValues = correlatedNetwork.run()
                        if (correlatedValues != null)
                            floatArrayOf(pid.third, it.value.toPidFloat(), correlatedValues)
                        else
                            null

                    } else*/
                    floatArrayOf(recentVal)
            )

            if (trainArray != null)
                net.fit(trainArray, Nd4j.create(floatArrayOf(out)))
            else {
                failureCount++
                if (failureCount > MAX_ALLOWED_FAILURE_COUNT)
                    throw UninitializedPropertyAccessException(
                            "Correlated inputs are not attaining any samples. " +
                                    "Correlated Inputs must be sampling and have previously sampled data")
            }

            kp = net.outputLayer.params().getFloat(0, 0)
            ki = net.outputLayer.params().getFloat(0, 1)
            kd = net.outputLayer.params().getFloat(0, 2)

            /*println("o0:${out.getFloat(0)} o1:${out.getFloat(1)} o2:${out.getFloat(2)}")
            println("p0:${posO.getFloat(0)} p1:${posO.getFloat(1)} p2:${posO.getFloat(2)}")
            println("n0:${negO.getFloat(0)} n1:${negO.getFloat(1)} n2:${negO.getFloat(2)}")

            kp = negO.getFloat(0)
            ki = negO.getFloat(1)
            kd = negO.getFloat(2)*/

            println("p:$kp i:$ki d:$kd")

            previousTime = it.instant
            yield()

            /*output.setOutput(
                    postProcessor(
                            targetInput,
                            correlatedInputs,
                            DaqcQuantity.of((out)(outputUnit))
                    )
            )*/
        }
    }

    private fun getTime(instant: Instant): Double =
            if (previousTime.isBefore(instant))
                Duration.between(previousTime, instant).toMillis().toDouble()
            else
                DEFAULT_TIME_VALUE


    private fun runPid(desiredValue: T, data: ValueInstant<DaqcValue>): Triple<Float, Float, Float> {

        val recentVal = data.value.toPidFloat()

        val time = getTime(data.instant)

        error = desiredValue.toPidFloat() - recentVal

        integral += (error * time).toFloat()

        val derivative = error - previousError

        if (integral > WINDUP_LIMIT)
            integral = WINDUP_LIMIT
        else if (integral < -WINDUP_LIMIT)
            integral = -WINDUP_LIMIT

        previousError = error

        return Triple(error, integral, derivative)
    }

    /**
     * Sets the target output of the NNPID controller.
     *
     * @param setting The target output.
     *
     * @throws IllegalArgumentException If correlated input values are null
     */
    override fun setOutput(setting: T) {
        //TODO This can occasionally consume additional resources if called during NN training.
        if (listenJob?.isActive == true) {
            listenJob?.cancel()
        }

        _broadcastChannel.offer(setting.now())

        targetInput.activate()
        correlatedInputs.forEach(Input<DaqcValue>::activate)

        runJob(setting)
    }

    override fun deactivate() {
        output.deactivate()

        listenJob?.cancel()

        _isActivate = false
    }

    companion object {
        private const val MAX_ALLOWED_FAILURE_COUNT = 20

        private const val PID_ITERATIONS = 10

        private const val PID_LEARNING_RATE = .5

        private const val PID_HIDDEN_SIZE = 3

        private const val PID_OUT_SIZE = 1

        private const val WEIGHT_UPPER_BOUND = 1.0

        private const val WEIGHT_LOWER_BOUND = 0.0

        private const val DEFAULT_TIME_VALUE = .00005

        private const val WINDUP_LIMIT = 20.0f

        inline operator fun <T : DaqcValue, reified O : Quantity<O>> invoke(
                targetInput: Input<T>,
                output: QuantityOutput<O>,
                noinline postProcessor: (Input<T>, Array<out Input<*>>, DaqcQuantity<O>) -> DaqcQuantity<O>,
                vararg correlatedInputs: Input<DaqcValue>): NnpidController<T, O> =
                NnpidController<T, O>(
                        targetInput = targetInput,
                        outputUnit = Units.getInstance().getUnit(O::class.java),
                        output = output,
                        postProcessor = postProcessor,
                        correlatedInputs = *correlatedInputs
                )

        inline operator fun <T : DaqcValue, reified O : Quantity<O>> invoke(
                targetInput: Input<T>,
                output: QuantityOutput<O>,
                vararg correlatedInputs: Input<DaqcValue>): NnpidController<T, O> =
                NnpidController<T, O>(
                        targetInput = targetInput,
                        outputUnit = Units.getInstance().getUnit(O::class.java),
                        output = output,
                        postProcessor = { _, _, out -> out },
                        correlatedInputs = *correlatedInputs
                )
    }
}