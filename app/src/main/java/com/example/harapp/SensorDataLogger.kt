package com.example.harapp

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class SensorDataLogger(context: Context) : SensorEventListener {
    private lateinit var measuredListener : SensorMeasurementListener

    interface SensorMeasurementListener{
        fun onSensorMeasurementSuccess(sensorDataArray: FloatArray)
    }

    fun setSensorMeasurementListener(listener: SensorMeasurementListener){
        this.measuredListener = listener
    }

    private var arrayX: MutableList<Float> = mutableListOf()
    private var arrayY: MutableList<Float> = mutableListOf()
    private var arrayZ: MutableList<Float> = mutableListOf()
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var count = 0
    override fun onSensorChanged(event: SensorEvent?) {
        if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
            arrayX.add(event.values[0])
            arrayY.add(event.values[1])
            arrayZ.add(event.values[2])
            count++
        }
        if(count>255){
            val sensorDataArray = arrayX + arrayY + arrayZ
//            Log.d(TAG,sensorDataArray.size.toString())
            count = 0
            measuredListener.onSensorMeasurementSuccess(sensorDataArray.toFloatArray())
            arrayX.clear()
            arrayY.clear()
            arrayZ.clear()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        Log.d(TAG,"sensor accuracy changed")
    }

    fun startSensorLogger() {
        sensorManager.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_FASTEST)
        Log.d(TAG,"sensor start")
    }

    fun stopSensorLogger(){
        sensorManager.unregisterListener(this)
    }

    companion object{
        private const val TAG="SensorDataLogger"
    }
}