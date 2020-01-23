package com.nexenio.seamlessauthenticationintegrationsample.health;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SampleApplication;
import com.nexenio.seamlessauthenticationintegrationsample.health.gatt.GattHealthManager;
import com.nexenio.seamlessauthenticationintegrationsample.health.sblec.SblecHealthManager;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorListActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * A fragment representing a single Authenticator health check screen. This fragment is either
 * contained in a {@link AuthenticatorListActivity} in two-pane mode (on tablets) or a {@link
 * AuthenticatorHealthActivity} on handsets.
 */
public class AuthenticatorHealthFragment extends Fragment {

    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String KEY_AUTHENTICATOR_ID = "authenticator_id";

    private static final long SBLEC_HEALTH_CHECK_DELAY = TimeUnit.SECONDS.toMillis(1);
    private static final long SBLEC_HEALTH_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(30);
    private static final long SBLEC_HEALTH_CHECK_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private static final long GATT_HEALTH_CHECK_DELAY = SBLEC_HEALTH_CHECK_DELAY + SBLEC_HEALTH_CHECK_TIMEOUT;
    private static final long GATT_HEALTH_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(30);
    private static final long GATT_HEALTH_CHECK_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    protected SampleApplication application;

    private UUID authenticatorId;

    private SeamlessAuthenticatorDetector authenticatorDetector;
    private Disposable healthMonitorDisposable;

    private SeamlessAuthenticator authenticator;

    private SblecHealthManager sblecHealthManager;
    private GattHealthManager gattHealthManager;

    private CollapsingToolbarLayout appBarLayout;

    private RelativeLayout sblecContainer;
    private TextView sblecDescriptionTextView;
    private ImageView sblecIconImageView;

    private RelativeLayout gattContainer;
    private TextView gattDescriptionTextView;
    private ImageView gattIconImageView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public AuthenticatorHealthFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SampleApplication application = (SampleApplication) getActivity().getApplication();
        authenticatorDetector = application.getAuthenticatorDetector();

        if (getArguments().containsKey(KEY_AUTHENTICATOR_ID)) {
            String idArgument = getArguments().getString(KEY_AUTHENTICATOR_ID);
            authenticatorId = UUID.fromString(idArgument);
            Timber.d("Authenticator ID: %s", authenticatorId);
        }

        sblecHealthManager = new SblecHealthManager(authenticatorId);
        gattHealthManager = new GattHealthManager(authenticatorId);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.authenticator_health, container, false);

        appBarLayout = getActivity().findViewById(R.id.toolbar_layout);

        sblecContainer = rootView.findViewById(R.id.sblecContainer);
        sblecDescriptionTextView = rootView.findViewById(R.id.sblecDescription);
        sblecIconImageView = rootView.findViewById(R.id.sblecIcon);

        gattContainer = rootView.findViewById(R.id.gattContainer);
        gattDescriptionTextView = rootView.findViewById(R.id.gattDescription);
        gattIconImageView = rootView.findViewById(R.id.gattIcon);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        application = (SampleApplication) getActivity().getApplication();
        startHealthMonitoring();
    }

    @Override
    public void onStop() {
        stopHealthMonitoring();
        super.onStop();
    }

    private void startHealthMonitoring() {
        Timber.d("startHealthMonitoring() called");

        indicateSblecUnknown();
        indicateGattUnknown();

        healthMonitorDisposable = Completable.mergeArray(
                monitorSblecHealth().subscribeOn(Schedulers.io()),
                monitorGattHealth().subscribeOn(Schedulers.io())
        ).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    indicateSblecUnknown();
                    indicateGattUnknown();
                })
                .subscribe(
                        () -> Timber.i("Health monitoring completed"),
                        throwable -> Timber.w(throwable, "Unable to monitor health")
                );
    }

    private void stopHealthMonitoring() {
        Timber.d("stopHealthMonitoring() called");
        if (healthMonitorDisposable != null && !healthMonitorDisposable.isDisposed()) {
            healthMonitorDisposable.dispose();
        }
    }

    /*
        SBLEC Monitoring
     */

    private Completable monitorSblecHealth() {
        Completable monitor = Observable.interval(SBLEC_HEALTH_CHECK_DELAY, SBLEC_HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .doOnNext(count -> {
                    Timber.d("Initiating SBLEC health check # %d", count + 1);
                    indicateSblecChecking();
                })
                .flatMapCompletable(count -> sblecHealthManager.getHealthCheckResult()
                        .timeout(SBLEC_HEALTH_CHECK_TIMEOUT, TimeUnit.MILLISECONDS)
                        .doOnSuccess(this::indicateSblecHealthy)
                        .doOnError(this::indicateSblecUnhealthy)
                        .ignoreElement()
                        .onErrorComplete())
                .doFinally(this::indicateSblecUnknown);

        return sblecHealthManager.initialize(getContext())
                .andThen(monitor);
    }

    private void indicateSblecChecking() {
        Timber.d("indicateSblecChecking() called");
        sblecDescriptionTextView.setText(R.string.monitoring_checking);
        sblecIconImageView.setImageResource(R.drawable.ic_autorenew_black_24dp);
    }

    private void indicateSblecHealthy(@NonNull HealthCheckResult healthCheckResult) {
        Timber.d("indicateSblecHealthy() called with: healthCheckResult = [%s]", healthCheckResult);
        sblecDescriptionTextView.setText(R.string.monitoring_healthy);
        sblecIconImageView.setImageResource(R.drawable.ic_check_black_24dp);
        sblecContainer.setBackgroundResource(R.color.monitoring_healthy);
        trackHealth("SBLEC", true);
    }

    private void indicateSblecUnhealthy(@NonNull Throwable throwable) {
        Timber.w("indicateSblecUnhealthy() called with: throwable = [%s]", throwable);
        sblecDescriptionTextView.setText(R.string.monitoring_unhealthy);
        sblecIconImageView.setImageResource(R.drawable.ic_close_black_24dp);
        sblecContainer.setBackgroundResource(R.color.monitoring_unhealthy);
        trackHealth("SBLEC", false);
    }

    private void indicateSblecUnknown() {
        Timber.d("indicateSblecUnknown() called");
        sblecDescriptionTextView.setText(R.string.monitoring_unknown);
        sblecIconImageView.setImageResource(R.drawable.ic_sync_problem_black_24dp);
        sblecContainer.setBackgroundResource(R.color.monitoring_unknown);
    }

    /*
        GATT Monitoring
     */

    private Completable monitorGattHealth() {
        Completable monitor = Observable.interval(GATT_HEALTH_CHECK_DELAY, GATT_HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .doOnNext(count -> {
                    Timber.d("Initiating GATT health check # %d", count + 1);
                    indicateGattChecking();
                })
                .flatMapCompletable(count -> gattHealthManager.getHealthCheckResult()
                        .timeout(GATT_HEALTH_CHECK_TIMEOUT, TimeUnit.MILLISECONDS)
                        .doOnSuccess(this::indicateGattHealthy)
                        .doOnError(this::indicateGattUnhealthy)
                        .ignoreElement()
                        .onErrorComplete())
                .doFinally(this::indicateGattUnknown);

        return gattHealthManager.initialize(getContext())
                .andThen(monitor);
    }

    private void indicateGattChecking() {
        Timber.d("indicateGattChecking() called");
        gattDescriptionTextView.setText(R.string.monitoring_checking);
        gattIconImageView.setImageResource(R.drawable.ic_autorenew_black_24dp);
    }

    private void indicateGattHealthy(@NonNull HealthCheckResult healthCheckResult) {
        Timber.d("indicateGattHealthy() called with: healthCheckResult = [%s]", healthCheckResult);
        gattDescriptionTextView.setText(R.string.monitoring_healthy);
        gattIconImageView.setImageResource(R.drawable.ic_check_black_24dp);
        gattContainer.setBackgroundResource(R.color.monitoring_healthy);
        trackHealth("GATT", true);
    }

    private void indicateGattUnhealthy(@NonNull Throwable throwable) {
        Timber.w("indicateGattUnhealthy() called with: throwable = [%s]", throwable);
        gattDescriptionTextView.setText(R.string.monitoring_unhealthy);
        gattIconImageView.setImageResource(R.drawable.ic_close_black_24dp);
        gattContainer.setBackgroundResource(R.color.monitoring_unhealthy);
        trackHealth("GATT", false);
    }

    private void indicateGattUnknown() {
        Timber.d("indicateGattUnknown() called");
        gattDescriptionTextView.setText(R.string.monitoring_unknown);
        gattIconImageView.setImageResource(R.drawable.ic_sync_problem_black_24dp);
        gattContainer.setBackgroundResource(R.color.monitoring_unknown);
    }

    @SuppressLint("CheckResult")
    private void trackHealth(@NonNull String protocol, boolean healthy) {
        Completable.fromAction(
                () -> {
                    URL url = new URL("https://feedbackseamless.nexenio.com/api/feedback");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);

                    String id = protocol.toLowerCase() + "-" + (healthy ? "healthy" : "unhealthy") + "-" + authenticatorId;
                    String name = healthy ? "Healthy" : "Unhealthy";
                    String path = "seamless-gate/" + id;

                    JSONObject selectedOption = new JSONObject();
                    selectedOption.put("id", id);
                    selectedOption.put("name", name);
                    selectedOption.put("path", path);

                    JSONObject feedback = new JSONObject();
                    feedback.put("sessionId", application.getDeviceId());
                    feedback.put("selectedOption", selectedOption);

                    DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                    os.writeBytes(feedback.toString());

                    os.flush();
                    os.close();

                    connection.disconnect();
                })
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.v("%s health tracked", protocol),
                        throwable -> Timber.w(throwable, "Unable to track %s health", protocol)
                );
    }

}
