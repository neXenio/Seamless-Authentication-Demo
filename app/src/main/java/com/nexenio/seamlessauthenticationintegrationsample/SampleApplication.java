package com.nexenio.seamlessauthenticationintegrationsample;

import android.app.Application;

import com.nexenio.seamlessauthentication.SeamlessAuthentication;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;

import java.util.UUID;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class SampleApplication extends Application {

    private UUID userId = UUID.randomUUID();
    private UUID deviceId = UUID.randomUUID();

    @NonNull
    private SeamlessAuthenticatorDetector authenticatorDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        authenticatorDetector = SeamlessAuthentication.createDetector(this);
    }

    @NonNull
    public SeamlessAuthenticatorDetector getAuthenticatorDetector() {
        return authenticatorDetector;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

}
