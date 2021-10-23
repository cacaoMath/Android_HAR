package com.example.harapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() , SensorDataLogger.TestListener{

    private var predictedResultTv: TextView? = null
    private var predictionButton: Button? = null
    private var harClassifier = HAR(this)
    private lateinit var sensorDataLogger: SensorDataLogger
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        predictedResultTv = findViewById(R.id.predictionResultTv)
        predictionButton = findViewById(R.id.predictionButton)
        sensorDataLogger = SensorDataLogger(this)

        predictionButton?.setOnClickListener{
//            activityRecognition()
        }

        harClassifier
            .initialize()
            .addOnFailureListener { e -> Log.e(TAG, "Error to setting up activity classifier.", e) }

        val sensorDataLogger = SensorDataLogger(this)
        sensorDataLogger.setListener(this)
        sensorDataLogger.startSensorLogger()

    }

    override fun onDestroy() {
        harClassifier.close()
        sensorDataLogger.stopSensorLogger()
        super.onDestroy()
    }

    @SuppressLint("StringFormatInvalid")
    private fun activityRecognition(sensorDataArray: FloatArray){


        harClassifier
            .predictionAsync(signals = sensorDataArray)
            .addOnSuccessListener { resultText -> predictedResultTv?.text = resultText }
            .addOnFailureListener { e ->
                predictedResultTv?.text = getString(
                    R.string.classification_error_message,
                    e.localizedMessage
                )
                Log.e(TAG, "Error classifying drawing.", e)
            }
    }

    companion object{
        private const val TAG = "MainActivity"
    }

    override fun onSuccess(sensorDataArray: FloatArray) {

        activityRecognition(sensorDataArray)

    }
}