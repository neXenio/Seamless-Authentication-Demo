package com.nexenio.seamlessauthenticationintegrationsample;

import android.app.Application;

import com.nexenio.seamlessauthentication.SeamlessAuthentication;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class SampleApplication extends Application {

    @NonNull
    private SeamlessAuthenticatorDetector authenticatorDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
        this.authenticatorDetector = SeamlessAuthentication.createDetector(this);
    }

    @NonNull
    public SeamlessAuthenticatorDetector getAuthenticatorDetector() {
        return authenticatorDetector;
    }

}
