package com.nexenio.seamlessauthenticationintegrationsample;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.FrameLayout;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.concurrent.TimeUnit;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * An activity that uses a {@link SeamlessAuthenticatorDetector} to detect {@link
 * SeamlessAuthenticator}s.
 */
public abstract class SeamlessAuthenticationActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_ENABLE_LOCATION_SERVICES = 2;

    protected SampleApplication application;

    protected RxPermissions rxPermissions;
    protected SeamlessAuthenticatorDetector authenticatorDetector;
    protected Disposable authenticatorDetectorDisposable;

    protected CoordinatorLayout coordinatorLayout;
    protected CollapsingToolbarLayout toolbarLayout;
    protected Toolbar toolbar;
    protected FrameLayout progressBarFrameLayout;
    protected FloatingActionButton fab;

    private Snackbar statusSnackbar;
    private Snackbar errorSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (SampleApplication) getApplication();
        authenticatorDetector = application.getAuthenticatorDetector();
        rxPermissions = new RxPermissions(this);

        setContentView();
        initializeViews();

        indicateDetectionStopped();
        startSeamlessAuthenticatorDetection();
    }

    protected abstract void setContentView();

    @CallSuper
    protected void initializeViews() {
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        progressBarFrameLayout = findViewById(R.id.progressBarFrameLayout);
        fab = findViewById(R.id.fab);

        toolbarLayout = findViewById(R.id.toolbar_layout);
        toolbarLayout.setTitle(getTitle());

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);

        statusSnackbar = Snackbar.make(coordinatorLayout, R.string.status_unknown, Snackbar.LENGTH_SHORT);
        errorSnackbar = Snackbar.make(coordinatorLayout, R.string.error_unknown, Snackbar.LENGTH_SHORT);
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        //startSeamlessAuthenticatorDetection();

        Flowable.timer(1, TimeUnit.SECONDS)
                .ignoreElements()
                .andThen(Completable.fromAction(this::checkPermissions))
                .subscribe();
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();
        //stopSeamlessAuthenticatorDetection();
    }

    @Override
    protected void onDestroy() {
        stopSeamlessAuthenticatorDetection();
        super.onDestroy();
    }

    /*
        Seamless authenticator detection
     */

    @CallSuper
    protected void startSeamlessAuthenticatorDetection() {
        Timber.d("startSeamlessAuthenticatorDetection() called");
        if (authenticatorDetectorDisposable != null && !authenticatorDetectorDisposable.isDisposed()) {
            Timber.w("Not starting seamless authenticator detection, already running");
            return;
        }
        authenticatorDetectorDisposable = authenticatorDetector.detect()
                .ignoreElements()
                .doOnSubscribe(subscription -> runOnUiThread(this::indicateDetectionStarted))
                .doFinally(() -> runOnUiThread(this::indicateDetectionStopped))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Timber.i("Seamless authenticator detection completed"),
                        this::performSeamlessAuthenticationTroubleshooting
                );
    }

    @CallSuper
    protected void stopSeamlessAuthenticatorDetection() {
        Timber.d("stopSeamlessAuthenticatorDetection() called");
        if (authenticatorDetectorDisposable != null && !authenticatorDetectorDisposable.isDisposed()) {
            authenticatorDetectorDisposable.dispose();
        }
    }

    @CallSuper
    protected void indicateDetectionStarted() {
        Timber.d("indicateDetectionStarted() called");
        progressBarFrameLayout.setVisibility(View.VISIBLE);
        fab.setOnClickListener(view -> stopSeamlessAuthenticatorDetection());
        fab.setImageResource(R.drawable.ic_pause_black_24dp);

        errorSnackbar.dismiss();
        statusSnackbar.dismiss();
        statusSnackbar = Snackbar.make(coordinatorLayout, R.string.status_detection_started, Snackbar.LENGTH_LONG);
        statusSnackbar.show();
    }

    @CallSuper
    protected void indicateDetectionStopped() {
        Timber.d("indicateDetectionStopped() called");
        progressBarFrameLayout.setVisibility(View.GONE);
        fab.setOnClickListener(view -> startSeamlessAuthenticatorDetection());
        fab.setImageResource(R.drawable.ic_autorenew_black_24dp);

        statusSnackbar.dismiss();
        if (errorSnackbar.getView().getVisibility() != View.VISIBLE) {
            statusSnackbar = Snackbar.make(coordinatorLayout, R.string.status_detection_stopped, Snackbar.LENGTH_LONG);
            statusSnackbar.show();
        }
    }

    private void performSeamlessAuthenticationTroubleshooting(@NonNull Throwable throwable) {
        Timber.w(throwable, "Unable to detect seamless authenticators");

        checkPermissions();
        checkBluetoothEnabled();
        checkLocationServicesEnabled();
    }

    /*
        Permissions
     */

    private void checkPermissions() {
        Timber.d("checkPermissions() called");
        int bluetoothPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (bluetoothPermission != PackageManager.PERMISSION_GRANTED || locationPermission != PackageManager.PERMISSION_GRANTED) {
            showMissingPermissionError();
        }
    }

    private void showMissingPermissionError() {
        Timber.d("showMissingPermissionError() called");
        errorSnackbar.dismiss();
        errorSnackbar = Snackbar.make(coordinatorLayout, R.string.error_missing_permissions, Snackbar.LENGTH_INDEFINITE);
        errorSnackbar.setAction(R.string.action_grant_permission, v -> requestMissingPermissions());
        errorSnackbar.show();
    }

    @SuppressLint("CheckResult")
    private void requestMissingPermissions() {
        Timber.d("requestMissingPermissions() called");
        rxPermissions.request(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe(permissionsGranted -> {
                    if (permissionsGranted) {
                        startSeamlessAuthenticatorDetection();
                    } else {
                        Timber.w("Required permissions not granted");
                        showMissingPermissionError();
                    }
                });
    }

    /*
        Bluetooth
     */

    private void checkBluetoothEnabled() {
        Timber.d("checkBluetoothEnabled() called");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            showBluetoothDisabledError();
        }
    }

    private void showBluetoothDisabledError() {
        Timber.d("showBluetoothDisabledError() called");
        errorSnackbar.dismiss();
        errorSnackbar = Snackbar.make(coordinatorLayout, R.string.error_bluetooth_disabled, Snackbar.LENGTH_INDEFINITE);
        errorSnackbar.setAction(R.string.action_enable, v -> enableBluetooth());
        errorSnackbar.show();
    }

    private void enableBluetooth() {
        Timber.d("enableBluetooth() called");
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
    }

    /*
        Location Services
     */

    private void checkLocationServicesEnabled() {
        Timber.d("checkLocationServicesEnabled() called");
        boolean enabled;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            enabled = locationManager.isLocationEnabled();
        } else {
            int locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            enabled = locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }
        if (!enabled) {
            showLocationServicesDisabledError();
        }
    }

    private void showLocationServicesDisabledError() {
        Timber.d("showLocationServicesDisabledError() called");
        errorSnackbar.dismiss();
        errorSnackbar = Snackbar.make(coordinatorLayout, R.string.error_location_services_disabled, Snackbar.LENGTH_INDEFINITE);
        errorSnackbar.setAction(R.string.action_enable, v -> enableLocationServices());
        errorSnackbar.show();
    }

    private void enableLocationServices() {
        Timber.d("enableLocationServices() called");
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, REQUEST_ENABLE_LOCATION_SERVICES);
    }

}
