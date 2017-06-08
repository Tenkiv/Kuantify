package general

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject

/**
 * Created by tenkiv on 5/15/17.
 */
class RecorderTest: io.kotlintest.specs.StringSpec() {
    init{
        "JSON Recording Test"{

            val gibberingSensor = GenericGibberingSensor()

            var completed = false

            val file = java.io.File("./TestRecording.json")

            //No False Positives
            if(file.exists()){
                file.delete()
            }

            val recorder = com.tenkiv.daqc.recording.disk.JSONRecorder(file.path,
                    recordingObjects = mapOf(Pair(gibberingSensor, "Gibbering Sensor")))

            recorder.start()

            Thread.sleep(5000)

            recorder.stop()

            Thread.sleep(1000)

            try {
                val json = com.beust.klaxon.Parser().parse(file.path) as com.beust.klaxon.JsonArray<JsonArray<JsonObject>>

                json.forEach(::println)

                completed = json[0].size > 0

            }catch (exception: Exception){
                println("Almost certainly Failed.")
            }

            assert(completed)

            // Cleanup
            if(file.exists()){
                file.delete()
            }
        }

        "Analog Memory Recorder Test"{

            val gibberingSensor = AnalogGibberingSensor()

            val recorder = com.tenkiv.daqc.recording.memory.AnalogMemoryRecorder(gibberingSensor, 10, "")

            recorder.start()

            Thread.sleep(10000)

            recorder.stop()

            gibberingSensor.cancel()

            println("Median "+recorder.median())

            println("Avg "+recorder.average())

        }

        "Digital Memory Recorder Test"{

            val gibberingSensor = PredicatbleSensor()

            val recorder = com.tenkiv.daqc.recording.memory.DigitalMemoryRecorder(gibberingSensor, 10, "")

            recorder.start()

            Thread.sleep(10000)

            recorder.stop()

            println("Asserting Median is On")
            assert(recorder.median().second!!.isOn)
            println("Median "+recorder.median())

            println("Asserting 80% on")
            assert(recorder.percentOn() == 0.8)
            println("Time On "+recorder.percentOn())

        }
    }
}