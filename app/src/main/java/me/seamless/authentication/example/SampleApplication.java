package me.seamless.authentication.example;

import android.app.Application;

import java.util.UUID;

import me.seamless.authentication.CommunicationUnitDetector;
import me.seamless.authentication.SeamlessAuthentication;
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
