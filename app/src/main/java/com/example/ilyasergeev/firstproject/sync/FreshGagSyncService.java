package com.example.ilyasergeev.firstproject.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class FreshGagSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static FreshGagSyncAdapter sFreshGagSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("FreshGagSyncService", "onCreate - FreshGagSyncService");
        synchronized (sSyncAdapterLock) {
            if (sFreshGagSyncAdapter == null) {
                sFreshGagSyncAdapter = new FreshGagSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sFreshGagSyncAdapter.getSyncAdapterBinder();
    }
}