package com.nexenio.seamlessauthenticationintegrationsample;

import android.app.Application;

import com.nexenio.seamlessauthentication.SeamlessAuthentication;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;

import java.util.concurrent.TimeUnit;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SampleApplication extends Application {

    private static final long RETRY_DELAY = TimeUnit.SECONDS.toMillis(7);

    @NonNull
    private SeamlessAuthenticatorDetector authenticatorDetector;

    @Nullable
    private Disposable authenticatorDetectorDisposable;

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());

        authenticatorDetector = SeamlessAuthentication.createDetector(this);
        startSeamlessAuthenticatorDetection();
    }

    @Override
    public void onTerminate() {
        stopSeamlessAuthenticatorDetection();
        super.onTerminate();
    }

    @CallSuper
    protected void startSeamlessAuthenticatorDetection() {
        Timber.d("startSeamlessAuthenticatorDetection() called");
        authenticatorDetectorDisposable = authenticatorDetector.detect()
                .ignoreElements()
                .doOnError(throwable -> Timber.w("Unable to start seamless authenticator detection"))
                .retryWhen(throwableFlowable -> throwableFlowable.delay(RETRY_DELAY, TimeUnit.MILLISECONDS))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.i("Seamless authenticator detection completed"),
                        throwable -> Timber.w("Unable to start seamless authenticator detection")
                );
    }

    @CallSuper
    protected void stopSeamlessAuthenticatorDetection() {
        Timber.d("stopSeamlessAuthenticatorDetection() called");
        if (authenticatorDetectorDisposable != null && !authenticatorDetectorDisposable.isDisposed()) {
            authenticatorDetectorDisposable.dispose();
        }
    }

    @NonNull
    public SeamlessAuthenticatorDetector getAuthenticatorDetector() {
        return authenticatorDetector;
    }

}
