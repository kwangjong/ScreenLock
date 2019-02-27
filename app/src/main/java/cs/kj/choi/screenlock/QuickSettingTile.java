package cs.kj.choi.screenlock;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class QuickSettingTile extends TileService {

    public static final String TAG = "TileService";
    private Icon mIcon;
    private static Tile mTile;
    private static Intent mScreenSaver;

    // General service code.

    @Override
    public void onCreate() {
        super.onCreate();
        mIcon = Icon.createWithResource(this, R.drawable.ic_screen_lock);
        Log.d(TAG, "onCreate: ");
        updateTile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        updateTile();
        Log.d(TAG, "onDestroy: do nothing");
    }

    // Bound Service code & TileService code.

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
        Log.d(TAG, "onTileAdded: ");
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        Log.d(TAG, "onTileRemoved: ");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
        Log.d(TAG, "onStartListening: ");
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        updateTile();
        Log.d(TAG, "onStopListening: ");
    }

    @Override
    public void onClick() {
        super.onClick();
        if(isRunning(ScreenSaverService.class)) {
            stopService(mScreenSaver);
            mScreenSaver = null;
        } else {
            mScreenSaver = new Intent(QuickSettingTile.this, ScreenSaverService.class);
            startService(mScreenSaver);
        }
        updateTile();
        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        this.sendBroadcast(it);
        Log.d(TAG, "onClick: ");
    }

    private boolean isRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        Log.i("isRunning:","false");
        return false;
    }

    public void updateTile() {
        mTile = getQsTile();
        if (mTile != null) {
            if (isRunning(ScreenSaverService.class)) {
                mTile.setState(Tile.STATE_ACTIVE);
                mTile.setIcon(mIcon);
            } else {
                mTile.setState(Tile.STATE_INACTIVE);
            }
            mTile.updateTile();
        }
    }

    public static Tile getTile() {
        return mTile;
    }
}
