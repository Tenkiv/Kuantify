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

package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.Output
import org.tenkiv.daqc.hardware.definitions.channel.AnalogOutput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.networking.SharingStatus

interface ControlDevice : Device {

    val analogOutputs: List<AnalogOutput>

    val digitalOutputs: List<DigitalOutput>

    val hasAnalogOutputs: Boolean

    val hasDigitalOutputs: Boolean

    val sharedOutputs: MutableMap<SharingStatus, Output<DaqcValue>>

}