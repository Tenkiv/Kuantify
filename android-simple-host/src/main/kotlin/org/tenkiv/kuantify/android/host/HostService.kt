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

package org.tenkiv.kuantify.android.host

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.*
import androidx.databinding.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.server.*
import java.util.concurrent.*

class HostService : Service() {

    private val server = embeddedServer(Netty, port = RC.DEFAULT_PORT) { kuantifyHost() }
    private val device by lazy(LazyThreadSafetyMode.NONE) { LocalAndroidDevice.get(applicationContext) }

    //called only once when the service is first initialized
    override fun onCreate() {
        super.onCreate()

        val showActivityPI = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            0
        )

        val stopServiceIntent = Intent(applicationContext, ActionReceiver::class.java)
        val stopServicePI = PendingIntent.getBroadcast(
            this,
            0,
            stopServiceIntent,
            0
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kuantify Host Service")
            .setContentText("Kuantify is hosting device.")
            .setSmallIcon(R.drawable.ic_daqc_dude_notification_icon)
            .setContentIntent(showActivityPI)
            .addAction(R.drawable.ic_daqc_dude_notification_icon, "Stop Hosting", stopServicePI)
            .build()

        server.start()
        device.startHosting()

        startForeground(1, notification)
        isRunning.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        runBlocking { device.stopHosting() }
        server.stop(1, 1, TimeUnit.SECONDS)
        isRunning.set(false)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        val isRunning = ObservableBoolean(false)
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, HostService::class.java)
        context.stopService(serviceIntent)
    }
}