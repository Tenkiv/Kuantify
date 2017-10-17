package org.tenkiv.daqc.recording

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import org.tenkiv.coral.at
import org.tenkiv.daqc.BinaryState
import org.tenkiv.daqc.DigitalGibberingSensor
import java.time.Instant

class RecorderSpec : StringSpec() {

    init {
        "Memory Recorder Test"{

            val testData = listOf(
                    0.at(Instant.MAX),
                    1.at(Instant.now()),
                    2.at(Instant.now()),
                    3.at(Instant.now()),
                    4.at(Instant.now()),
                    5.at(Instant.MAX)
            )

            Thread.sleep(100)

            assert(testData.getDataInRange(Instant.MIN..Instant.now()).size == 4)

            val recorders = ArrayList<Recorder<BinaryState>>()
            for (i in 0..20) {
                recorders.add(
                        DigitalGibberingSensor().newBinaryStateRecorder(storageFrequency = StorageFrequency.All,
                                memoryDuration = StorageDuration.None,
                                diskDuration = StorageDuration.Forever)
                )
            }

            Thread.sleep(10000)

            runBlocking {
                recorders.forEach {
                    //it.stop(false)
                    println(it.getAllData().await())
                }
            }

            Thread.sleep(10000)
        }
    }
}