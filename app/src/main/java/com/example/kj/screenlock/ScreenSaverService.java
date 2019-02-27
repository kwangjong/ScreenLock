package com.example.kj.screenlock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

public class ScreenSaverService extends Service {

    private static final String TAG = "ScreenSaverService";
    private static ScreenSaverService mInstance;

    private WindowManager mWindowManager;
    private View mScreenSaverView;
    private ImageView background;
    private TextView doubleTap;
    private TextView batteryStatus;
    private ImageView batteryStatusImg;
    private Handler mHandler;
    private Runnable r;

    private SensorManager mSensorManager;
    private Sensor mProximity;
    private proximitySensorListener mProximitySensorListener;


    // setup BroadcastReceiver to update battery status
    public BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // check if charging
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
            //int rawLevel = intent.getIntExtra("level", -1);
            int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            //int scale = intent.getIntExtra("scale", -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int level = -1;
            if (rawLevel >= 0 && scale > 0) {
                level = (rawLevel * 100) / scale;
            }

            if(!isCharging) {
                if (level <= 20) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_20_black_24dp);
                } else if (level <= 30) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_30_black_24dp);
                } else if (level <= 50) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_50_black_24dp);
                } else if (level <= 60) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_60_black_24dp);
                } else if (level <= 80) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_80_black_24dp);
                } else if (level <= 90) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_90_black_24dp);
                } else if (level <= 100) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_full_black_24dp);
                }
            } else {
                if (level <= 20) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_charging_20_black_24dp);
                } else if (level <= 30) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_charging_30_black_24dp);
                } else if (level <= 50) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_charging_50_black_24dp);
                } else if (level <= 60) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_charging_60_black_24dp);
                } else if (level <= 80) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_charging_80_black_24dp);
                } else if (level <= 90) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_charging_90_black_24dp);
                } else if (level <= 100) {
                    batteryStatusImg.setImageResource(R.drawable.ic_battery_charging_full_black_24dp);
                }
            }
            batteryStatus.setText(level+"%");
            Log.d(TAG,"mBatteryLevelReceiver called");
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this; // used to communicate with activity.

        //Add the view to the window
        mScreenSaverView = LayoutInflater.from(this).inflate(R.layout.layout_screen_saver, null);
        mScreenSaverView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        background = mScreenSaverView.findViewById(R.id.background);
        background.setBackgroundColor(Color.argb(255,0,0,0));
        background.setAlpha((float) 244/255);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mScreenSaverView, params);

        // Setup battery status textview and imageview
        batteryStatus = mScreenSaverView.findViewById(R.id.battery_status);
        batteryStatusImg = mScreenSaverView.findViewById(R.id.battery);
        doubleTap = mScreenSaverView.findViewById(R.id.textView);
        batteryLevel();

        // schedule fadeOut when not touched for two seconds
        // deem brightness when fadeOut
        mHandler = new Handler();
        r = new Runnable()
        {
            public void run()
            {
                fadeOut(batteryStatus, 300);
                fadeOut(batteryStatusImg, 300);
                fadeOut(doubleTap,300);
                setBrightnessDeem();
            }
        };
        mHandler.postDelayed(r,3000);

        // setup proximity sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mProximitySensorListener = new proximitySensorListener();
        mSensorManager.registerListener(mProximitySensorListener, mProximity, SensorManager.SENSOR_DELAY_NORMAL);

        //onClickListener
        mScreenSaverView.setOnTouchListener(new View.OnTouchListener() {
            private static final long DOUBLE_PRESS_INTERVAL = 250; // in millis
            private long lastPressTime;
            private long pressTime;
            private Handler handleSingleTap;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // phone is in the pocket do not get touch input
                if(mProximitySensorListener.isBlocked())
                    return false;
                if(mHandler != null) {
                    mHandler.removeCallbacks(r);
                    mHandler = null;
                }
                if(MotionEvent.ACTION_DOWN == event.getAction()) {
                    fadeIn(batteryStatus,300);
                    fadeIn(batteryStatusImg,300);
                    fadeIn(doubleTap, 300);
                    revertBrightnessDeem();
                    pressTime = System.currentTimeMillis();
                    return true;
                } else if(MotionEvent.ACTION_UP == event.getAction()){
                    // If double click...
                    if (pressTime - lastPressTime <= DOUBLE_PRESS_INTERVAL) {
                        stopSelf();
                    }
                    else {
                        fadeOut(batteryStatus, 300);
                        fadeOut(batteryStatusImg, 300);
                        fadeOut(doubleTap, 300);
                        setBrightnessDeem();   //change brightness mode back to normal
                        lastPressTime = pressTime;
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScreenSaverView != null) mWindowManager.removeView(mScreenSaverView);
        if (mBatteryLevelReceiver != null) unregisterReceiver(mBatteryLevelReceiver);
        mSensorManager.unregisterListener(mProximitySensorListener);
        QuickSettingTile.getTile().setState(Tile.STATE_INACTIVE);
        if(mHandler != null) {
            mHandler.removeCallbacks(r);
            mHandler = null;
        }
        revertBrightnessDeem();
    }

    public static ScreenSaverService getInstance() {
        return mInstance;
    }

    private void batteryLevel() {
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBatteryLevelReceiver, batteryLevelFilter);
    }

    private void fadeOut(final View img, int millisec)
    {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(millisec);

        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                img.setVisibility(View.INVISIBLE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeOut);
    }

    private void fadeIn(final View img, int millisec)
    {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(millisec);

        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                img.setVisibility(View.VISIBLE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeIn);
    }

    private void setBrightnessDeem()
    {
        // Change the screen brightness change mode to manual
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        // Apply the screen brightness value to the system
        Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
    }

    private void revertBrightnessDeem()
    {
        // Change the screen brightness change mode to auto
    Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }
}
