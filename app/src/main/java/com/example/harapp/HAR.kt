package com.example.harapp

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.Throws

/**
 * https://developer.android.com/codelabs/digit-classifier-tflite
 * 参考にtfliteによる推論の実装
 */
class HAR(private val context: Context) {
    private var interpreter: Interpreter? = null

    var isInitialized = false
        private set

    /** Executor to run inference task in the background. */
    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    private var inputSignalWidth: Int = 0 // will be inferred from TF Lite model.
    private var inputSignalHeight: Int = 0 // will be inferred from TF Lite model.
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model.


    fun initialize(): Task<Void?> {
        val task = TaskCompletionSource<Void?>()
        executorService.execute {
            try {
                initializeInterpreter()
                task.setResult(null)
            } catch (e: IOException) {
                task.setException(e)
            }
        }
        return task.task
    }

    private fun initializeInterpreter(){
        //load tflite model
        val assetManager = context.assets
        val model = loadModelFile(assetManager, "HAR_model.tflite")
        val interpreter = Interpreter(model)
        //
        val inputShape = interpreter.getInputTensor(0).shape()
        inputSignalWidth = inputShape[1]
        inputSignalHeight = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE*inputSignalWidth*inputSignalHeight*SIGNAL_CHANNEL_SIZE

        this.interpreter = interpreter

        isInitialized = true
        Log.i(TAG, "Initialized TFLite interpreter.")
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager:AssetManager, fileName: String):ByteBuffer{
        val fileDescriptor = assetManager.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return  fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun prediction(signals: FloatArray): String {
        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }
        val byteBuffer = convertSignalToByteBuffer(signals)
        val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

        interpreter?.run(byteBuffer, output)

        val result = output[0]
        Log.d(TAG,result[0].toString())
        val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1

        val activity = when(maxIndex){
            0->"sit"
            1->"lie"
            2->"stand"
            3, 4 ->"walk"
            else ->"error"
        }

        return "Prediction Result: %s\nConfidence: %2f"
            .format(activity, result[maxIndex])
    }

    fun predictionAsync(signals: FloatArray):Task<String>{
        val task = TaskCompletionSource<String>()
        executorService.execute{
            val result = prediction(signals)
            task.setResult(result)
        }
        return  task.task
    }

    private fun convertSignalToByteBuffer(signals: FloatArray): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        for (signalValue in signals) {

            byteBuffer.putFloat(signalValue)
        }

        return byteBuffer
    }

    fun close() {
        executorService.execute {
            interpreter?.close()

            Log.d(TAG, "Closed TFLite interpreter.")
        }
    }

    companion object{
        private  const val TAG = "HAR"

        private const val FLOAT_TYPE_SIZE = 4
        private const val SIGNAL_CHANNEL_SIZE = 1

        private const val OUTPUT_CLASSES_COUNT = 5
    }
}