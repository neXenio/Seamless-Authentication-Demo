package com.nexenio.seamlessauthenticationintegrationsample;

import android.app.Application;

import com.nexenio.seamlessauthentication.CommunicationUnitDetector;
import com.nexenio.seamlessauthentication.SeamlessAuthentication;

import java.util.UUID;

import timber.log.Timber;

public class SampleApplication extends Application {

    private UUID userId = UUID.randomUUID();
    private UUID deviceId = UUID.randomUUID();

    private SeamlessAuthentication seamlessAuthentication;
    private CommunicationUnitDetector communicationUnitDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        seamlessAuthentication = SeamlessAuthentication.getInstance();

        seamlessAuthentication.initialize(this).blockingAwait();
        communicationUnitDetector = seamlessAuthentication.getCommunicationUnitDetector();
    }

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
