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

package org.tenkiv.kuantify.fs.gate.acquire

import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.acquire.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.networking.configuration.*

public abstract class FSRemoteAcquireChannel<T : DaqcData>(uid: String) : FSRemoteDaqcGate(uid), AcquireChannel<T> {
    private val startSamplingChannel = Channel<Ping>(Channel.RENDEZVOUS)
    public final override fun startSampling(): Unit =
        modifyConfiguration {
            command {
                startSamplingChannel.offer(Ping)
            }
            return@modifyConfiguration
        }

    public override fun routing(route: NetworkRoute<String>) {
        super.routing(route)
        route.add {
            bindPing(RC.START_SAMPLING) {
                send(source = startSamplingChannel)
            }
        }
    }

}