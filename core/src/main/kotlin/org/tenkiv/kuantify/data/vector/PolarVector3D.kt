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

package org.tenkiv.kuantify.data.vector

import org.tenkiv.kuantify.data.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import tec.units.indriya.unit.Units.*
import javax.measure.*
import javax.measure.quantity.*
import kotlin.math.*

/**
 * A [PolarVector3D] is a vector described in terms of two perpendicular polar planes with the same center point.
 */
public class PolarVector3D<Q : Quantity<Q>>(
    magnitude: ComparableQuantity<Q>,
    azimuth: ComparableQuantity<Angle>,
    incline: ComparableQuantity<Angle>,
    public val azimuthAxisLabel: String,
    public val inclineAxisLabel: String,
    public val azimuthPositiveDirection: CircularDirection,
    public val inclinePositiveDirection: CircularDirection
) : DaqcData {
    public val magnitude = magnitude.toDaqc()

    public val azimuth = azimuth.toDaqc()
    public val incline = incline.toDaqc()

    public override val size get() = 3

    public override fun toDaqcValues() = listOf(magnitude, incline, azimuth)

    private fun toComponentDoubles(): Triple<Double, Double, Double> {
        val magnitude = magnitude.valueToDouble()
        val incline = incline toDoubleIn RADIAN
        val azimuth = azimuth toDoubleIn RADIAN

        val xComponent = magnitude * cos(incline) * sin(azimuth)
        var yComponent = magnitude * cos(incline) * cos(azimuth)
        var zComponent = magnitude * sin(incline)

        if (azimuthPositiveDirection === CircularDirection.CLOCKWISE) yComponent = -yComponent
        if (inclinePositiveDirection === CircularDirection.CLOCKWISE) zComponent = -zComponent

        return Triple(xComponent, yComponent, zComponent)
    }

    public fun toComponents(): Components<Q> {
        val components = toComponentDoubles()
        val unit = magnitude.unit

        return Components(components.first(unit), components.second(unit), components.third(unit))
    }

    public operator fun times(scalar: Double): PolarVector3D<Q> = PolarVector3D(
        magnitude * scalar,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    public operator fun unaryPlus(): PolarVector3D<Q> = PolarVector3D<Q>(
        +magnitude,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    public operator fun unaryMinus(): PolarVector3D<Q> = PolarVector3D<Q>(
        -magnitude,
        azimuth,
        incline,
        azimuthAxisLabel,
        inclineAxisLabel,
        azimuthPositiveDirection,
        inclinePositiveDirection
    )

    public operator fun plus(other: PolarVector3D<Q>): PolarVector3D<Q> {
        val (thisX, thisY, thisZ) = toComponents()
        val (otherX, otherY, otherZ) = other.toComponents()

        val resultX = thisX + otherX
        val resultY = thisY + otherY
        val resultZ = thisZ + otherZ

        return fromComponents(
            resultX,
            resultY,
            resultZ,
            other.azimuthAxisLabel,
            other.inclineAxisLabel,
            other.azimuthPositiveDirection,
            other.inclinePositiveDirection
        )
    }

    public operator fun minus(other: PolarVector3D<Q>): PolarVector3D<Q> {
        val (thisX, thisY, thisZ) = toComponents()
        val (otherX, otherY, otherZ) = other.toComponents()

        val resultX = thisX - otherX
        val resultY = thisY - otherY
        val resultZ = thisZ - otherZ

        return fromComponents(
            resultX,
            resultY,
            resultZ,
            other.azimuthAxisLabel,
            other.inclineAxisLabel,
            other.azimuthPositiveDirection,
            other.inclinePositiveDirection
        )
    }

    public inline infix fun <reified RQ : Quantity<RQ>> dot(other: PolarVector3D<*>): ComparableQuantity<RQ> {
        val thisAzimuth = azimuth toDoubleIn RADIAN
        val otherAzimuth = other.azimuth toDoubleIn RADIAN
        val thisIncline = incline toDoubleIn RADIAN
        val otherIncline = other.incline toDoubleIn RADIAN

        return (magnitude * other.magnitude).asType<RQ>() *
                (sin(thisAzimuth) * sin(otherAzimuth) * cos(thisIncline - otherIncline) +
                        cos(thisAzimuth) * cos(otherAzimuth))
    }

    public inline infix fun <reified RQ : Quantity<RQ>> cross(other: PolarVector3D<*>): PolarVector3D<RQ> {
        val (thisX, thisY, thisZ) = toComponents()
        val (otherX, otherY, otherZ) = other.toComponents()

        val resultX = (thisY * thisZ).asType<RQ>() - (thisZ * thisY).asType()
        val resultY = (thisZ * otherX).asType<RQ>() - (thisX * otherZ).asType()
        val resultZ = (thisX * otherY).asType<RQ>() - (thisY * otherX).asType()

        return fromComponents(
            resultX,
            resultY,
            resultZ,
            azimuthAxisLabel,
            inclineAxisLabel,
            azimuthPositiveDirection,
            inclinePositiveDirection
        )
    }

    public data class Components<Q : Quantity<Q>>(
        val x: ComparableQuantity<Q>,
        val y: ComparableQuantity<Q>,
        val z: ComparableQuantity<Q>
    )

    public companion object {

        private fun <Q : Quantity<Q>> fromComponentDoubles(
            xComponent: Double,
            yComponent: Double,
            zComponent: Double,
            azimuthAxisLabel: String,
            inclineAxisLabel: String,
            azimuthPositiveDirection: CircularDirection,
            inclinePositiveDirection: CircularDirection,
            unit: PhysicalUnit<Q>
        ): PolarVector3D<Q> {
            val azimuthAngle = atan(xComponent / yComponent)
            val inclineAngle = acos(yComponent / zComponent)
            val magnitude = zComponent / sin(azimuthAngle)

            return PolarVector3D(
                magnitude(unit),
                azimuthAngle(RADIAN),
                inclineAngle(RADIAN),
                azimuthAxisLabel,
                inclineAxisLabel,
                azimuthPositiveDirection,
                inclinePositiveDirection
            )
        }

        public fun <Q : Quantity<Q>> fromComponents(
            xComponent: ComparableQuantity<Q>,
            yComponent: ComparableQuantity<Q>,
            zComponent: ComparableQuantity<Q>,
            azimuthAxisLabel: String,
            inclineAxisLabel: String,
            azimuthPositiveDirection: CircularDirection,
            inclinePositiveDirection: CircularDirection,
            unit: PhysicalUnit<Q> = xComponent.unit
        ): PolarVector3D<Q> {
            val xComponentDouble = xComponent toDoubleIn unit
            val yComponentDouble = yComponent toDoubleIn unit
            val zComponentDouble = zComponent toDoubleIn unit

            return fromComponentDoubles(
                xComponentDouble,
                yComponentDouble,
                zComponentDouble,
                azimuthAxisLabel,
                inclineAxisLabel,
                azimuthPositiveDirection,
                inclinePositiveDirection,
                unit
            )
        }
    }
}