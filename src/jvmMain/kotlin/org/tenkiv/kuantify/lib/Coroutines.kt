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

package org.tenkiv.kuantify.lib

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*

/**
 * Creates a new [CoroutineScope] which is identical to the current one but the [Job] of its context replaced with a
 * new [CompletableJob] that is a child of the current one.
 *
 * Generally used to make a scope that can be canceled independently of the parent scope.
 */
public fun CoroutineScope.withNewChildJob(): CoroutineScope = this + Job(this.coroutineContext[Job])

public class MutexValue<V : Any>(@PublishedApi internal val value: V, @PublishedApi internal val mutex: Mutex) {

    public suspend inline fun <R> withLock(block: (value: V) -> R): R = mutex.withLock {
        block(value)
    }

}
