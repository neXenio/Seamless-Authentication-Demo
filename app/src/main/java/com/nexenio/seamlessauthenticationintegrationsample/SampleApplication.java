package com.nexenio.seamlessauthenticationintegrationsample;

import android.app.Application;

import com.nexenio.seamlessauthentication.CommunicationUnitDetector;
import com.nexenio.seamlessauthentication.SeamlessAuthentication;

import java.util.UUID;

import androidx.annotation.NonNull;
import timber.log.Timber;

public class SampleApplication extends Application {

    private UUID userId = UUID.randomUUID();
    private UUID deviceId = UUID.randomUUID();

    @NonNull
    private CommunicationUnitDetector communicationUnitDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        communicationUnitDetector = SeamlessAuthentication.createDetector(this);
    }

    @NonNull
    public CommunicationUnitDetector getCommunicationUnitDetector() {
        return communicationUnitDetector;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

}
