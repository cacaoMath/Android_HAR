package com.example.harapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.example.harapp.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class MainActivity : AppCompatActivity() , SensorDataLogger.SensorMeasurementListener{

    private lateinit var recognitionResultTv: TextView
    private var harClassifier = HAR(this)
    private lateinit var sensorDataLogger: SensorDataLogger
    private var isSensing = false
    private lateinit var lineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        val sensingButton = mainBinding.sensingButton
        recognitionResultTv = mainBinding.predictionResultTv
        lineChart = mainBinding.lineChart

        sensorDataLogger = SensorDataLogger(this)

        harClassifier
            .initialize()
            .addOnFailureListener { e -> Log.e(TAG, "Error to setting up activity classifier.", e) }

        val sensorDataLogger = SensorDataLogger(this)
        sensorDataLogger.setSensorMeasurementListener(this)

        sensingButton.setOnClickListener{
            if(!isSensing){
                sensorDataLogger.startSensorLogger()
                sensingButton.text = getString(R.string.stop)
                isSensing = true
            }else{
                sensorDataLogger.stopSensorLogger()
                sensingButton.text = getString(R.string.start)
                recognitionResultTv.text = getString(R.string.standby)
                isSensing = false
            }
        }
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
        .addOnSuccessListener { resultTxt -> recognitionResultTv.text = resultTxt }
        .addOnFailureListener { e ->
            recognitionResultTv.text = getString(
                R.string.classification_error_message,
                e.localizedMessage
            )
            Log.e(TAG, "Error activity recognition.", e)
        }
    }
    override fun onSensorMeasurementSuccess(sensorDataArray: FloatArray) {
        activityRecognition(sensorDataArray)
        val entryX: MutableList<Entry> = mutableListOf()
        val entryY: MutableList<Entry> = mutableListOf()
        val entryZ: MutableList<Entry> = mutableListOf()

        Log.d(TAG,"${sensorDataArray.toList()}")
        for((idx, array) in sensorDataArray.toList().chunked(256).withIndex()){
            when(idx){
                0 -> {
                    var i = 0
                    array.forEach {
                        entryX.add(Entry(i.toFloat(), it))
//                        Log.d(TAG,"${i.toFloat()}, $array")
                        i++
                    }
                }
                1->{
                    var i = 0
                    array.forEach {
                        entryY.add(Entry(i.toFloat(), it))
                        i++

                    }
                }
                2->{
                    var i = 0
                    array.forEach {
                        entryZ.add(Entry(i.toFloat(), it))
                        i++

                    }
                }
            }
        }

        val lineDatasetX = LineDataSet(entryX, "X")
        val lineDatasetY = LineDataSet(entryY, "Y")
        val lineDatasetZ = LineDataSet(entryZ, "Z")
        lineDatasetX.color = R.color.black
        lineDatasetY.color = R.color.cardview_shadow_start_color
        lineDatasetZ.color = R.color.design_default_color_primary
        val lineData = LineData(lineDatasetX,lineDatasetY,lineDatasetZ)
        lineChart.data = lineData
        lineChart.invalidate()

    }

    companion object{
        private const val TAG = "MainActivity"
    }

}