package com.example.kj.screenlock;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

public class proximitySensorListener implements SensorEventListener {
    private static boolean isBlocked;
    private static final int SENSOR_SENSITIVITY = 4;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                Log.d("sjdjkskfjkjfs:","true");
                isBlocked = true;
            } else {
                isBlocked = false;
                Log.d("sjdjkskfjkjfs:","false");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public boolean isBlocked() {
        return isBlocked;
    }
}
