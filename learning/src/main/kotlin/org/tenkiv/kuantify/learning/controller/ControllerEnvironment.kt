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

package org.tenkiv.kuantify.learning.controller

import com.google.common.collect.*
import org.apache.commons.math3.distribution.*
import org.deeplearning4j.gym.*
import org.deeplearning4j.rl4j.mdp.*
import org.deeplearning4j.rl4j.space.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.recording.*

internal class ControllerEnvironment<T>(private val controller: LearningController<T>) :
    MDP<Encodable, Int, DiscreteSpace> where T : DaqcValue, T : Comparable<T> {

    val numQuantityOutputs = controller.outputs.count { it is RangedQuantityOutput<*> }

    val numBinaryStateOutputs = controller.outputs.count { it is BinaryStateOutput }

    private val actionHandlerList: List<OutputActionHandler> = kotlin.run {
        val listBuilder = ImmutableList.builder<OutputActionHandler>()

        controller.outputs.forEach {
            when (it) {
                is RangedQuantityOutput<*> -> listBuilder.add(QuantityOutputActionHandler(it))
                is BinaryStateOutput -> listBuilder.add(BinaryStateOutputActionHandler(it))
            }
        }

        listBuilder.build()
    }

    private val actionPermutationList: List<List<Int>> = Sets.cartesianProduct(
        actionHandlerList.map { it.actionSet }
    ).toList()

    private val rewardDistribution: NormalDistribution by lazy {
        val mean = controller.targetInput.updatable.getNormalisedDoubleOrNull()
        if (mean != null) {
            return@lazy NormalDistribution(mean, DIST_SD)
        } else {
            throw IllegalStateException("Tried to access rewardDistribution before setting the learning controller.")
        }
    }

    override fun getActionSpace() = DiscreteSpace(actionPermutationList.size)

    override fun getObservationSpace(): ObservationSpace<Encodable> {
        val size = getObservation().toArray().size
        val shape = IntArray(size) { 1 }

        return ArrayObservationSpace(shape)
    }

    override fun isDone(): Boolean {
        return !controller.isTransceiving.value
    }

    override fun newInstance(): MDP<Encodable, Int, DiscreteSpace> = ControllerEnvironment(controller)

    override fun reset(): Encodable {
        return getObservation()
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun step(action: Int): StepReply<Encodable> {
        actionPermutationList[action].forEachIndexed { index, individualAction ->
            actionHandlerList[index].takeAction(individualAction)
        }

        return StepReply(getObservation(), getReward(), isDone, null)
    }

    private fun getBinaryStateDoubles(io: Recorder<DaqcValue, RangedInput<*>>)
            : Double {

        return io.updatable.getNormalisedDoubleOrNull()!!
    }

    private fun getQuantityInputDoubles(io: Recorder<DaqcValue, RangedInput<*>>)
            : QuantityInputDoubles {
        val history = io.getDataInMemory()

        val min = io.updatable.valueRange.start.toDoubleInSystemUnit()
        val max = io.updatable.valueRange.endInclusive.toDoubleInSystemUnit()

        // This assumes the sample rate is consistent. We could change it to look at the time between each sample as
        // well if wed are worried about inconsistent sample rates.
        var totalChange = 0.0
        history.forEachIndexed { index, sample ->
            if (index + 1 <= history.size) {
                totalChange += sample.value.toDoubleInSystemUnit().normalTo(min..max) -
                        history[index + 1].value.toDoubleInSystemUnit().normalTo(min..max)
            }
        }
        //TODO: Change the number history.size is compared to to a constant defined somewhere else.
        val rateOfChange = if (history.size < 2) NONE else totalChange / history.size - 1

        val currentValue = io.updatable.getNormalisedDoubleOrNull()

        return QuantityInputDoubles(currentValue ?: NONE, rateOfChange)
    }

    private fun getQuantityOutputDoubles(io: Recorder<DaqcValue, RangedOutput<*>>)
            : Double {

        return io.updatable.getNormalisedDoubleOrNull()!!
    }

    private fun getReward(): Double {
        // Binary state
        val currentValue = controller.targetInput.updatable.valueOrNull?.value
        val targetValue = controller.valueOrNull!!.value
        if (currentValue is BinaryState) return if (currentValue == targetValue) 1.0 else -1.0

        // Quantity
        val currentValueDouble = controller.targetInput.updatable.getNormalisedDoubleOrNull()

        return if (currentValueDouble != null) rewardDistribution.density(currentValueDouble) - 1.0 else 0.0
    }

    fun getObservation(): Encodable {
        val observationsList = ArrayList<Double>()

        // Inputs and Outputs won't return null because we wait for them to initialise in LearningController.
        when (controller.targetInput.updatable.valueOrNull) {
            is BinaryState -> observationsList += getBinaryStateDoubles(controller.targetInput)
            is DaqcQuantity<*> -> observationsList += getQuantityInputDoubles(controller.targetInput)
        }

        controller.correlatedInputs.forEach {
            when (it.updatable.valueOrNull) {
                is BinaryState -> observationsList += getBinaryStateDoubles(controller.targetInput)
                is DaqcQuantity<*> -> observationsList += getQuantityInputDoubles(controller.targetInput)
            }
        }

        controller.outputs.forEach {
            observationsList += it.updatable.getNormalisedDoubleOrNull()!!
        }

        return Encodable { observationsList.toDoubleArray() }
    }

    private operator fun MutableList<Double>.plusAssign(quantityInputDoubles: QuantityInputDoubles) {
        this += quantityInputDoubles.currentValue
        this += quantityInputDoubles.rateOfChange
    }

    data class QuantityInputDoubles(val currentValue: Double, val rateOfChange: Double)

    companion object {
        private const val DIST_SD = 0.035
        private const val NONE = -1.0
    }
}