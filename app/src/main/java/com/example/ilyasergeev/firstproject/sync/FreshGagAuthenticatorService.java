package com.example.ilyasergeev.firstproject.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class FreshGagAuthenticatorService extends Service {

    private FreshGagAuthenticator mAuthenticator;

    @Override
    public void onCreate() {

        mAuthenticator = new FreshGagAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
