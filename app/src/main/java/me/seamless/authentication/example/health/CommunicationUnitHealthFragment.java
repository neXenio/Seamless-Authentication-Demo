package me.seamless.authentication.example.health;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import me.seamless.authentication.CommunicationUnit;
import me.seamless.authentication.CommunicationUnitDetector;
import me.seamless.authentication.example.R;
import me.seamless.authentication.example.SampleApplication;
import me.seamless.authentication.example.health.gatt.GattHealthManager;
import me.seamless.authentication.example.health.sblec.SblecHealthManager;
import me.seamless.authentication.example.overview.CommunicationUnitListActivity;
import me.seamless.sblec.internal.sender.advertiser.AdvertiserException;
import timber.log.Timber;

/**
 * A fragment representing a single {@link CommunicationUnit} health check screen. This fragment is
 * either contained in a {@link CommunicationUnitListActivity} in two-pane mode (on tablets) or a
 * {@link CommunicationUnitHealthActivity} on handsets.
 */
public class CommunicationUnitHealthFragment extends Fragment {

    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String KEY_COMMUNICATION_UNIT_ID = "communication_unit_id";

    private static final long SBLEC_HEALTH_CHECK_DELAY = TimeUnit.SECONDS.toMillis(1);
    private static final long SBLEC_HEALTH_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    private static final long SBLEC_HEALTH_CHECK_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    private static final long GATT_HEALTH_CHECK_DELAY = SBLEC_HEALTH_CHECK_DELAY + (SBLEC_HEALTH_CHECK_INTERVAL / 2);
    private static final long GATT_HEALTH_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    private static final long GATT_HEALTH_CHECK_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

    protected SampleApplication application;

    private UUID communicationUnitId;

    private CommunicationUnitDetector communicationUnitDetector;
    private Disposable healthMonitorDisposable;

    private CommunicationUnit communicationUnit;

    private SblecHealthManager sblecHealthManager;
    private GattHealthManager gattHealthManager;

    private CollapsingToolbarLayout appBarLayout;

    private RelativeLayout sblecContainer;
    private TextView sblecDescriptionTextView;
    private ImageView sblecIconImageView;
    private TextView sblecActiveDevicesTextView;
    private TextView sblecOperationalChipsTextView;

    private RelativeLayout gattContainer;
    private TextView gattDescriptionTextView;
    private ImageView gattIconImageView;
    private TextView gattActiveDevicesTextView;
    private TextView gattOperationalChipsTextView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public CommunicationUnitHealthFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SampleApplication application = (SampleApplication) getActivity().getApplication();
        communicationUnitDetector = application.getCommunicationUnitDetector();

        if (getArguments().containsKey(KEY_COMMUNICATION_UNIT_ID)) {
            String idArgument = getArguments().getString(KEY_COMMUNICATION_UNIT_ID);
            communicationUnitId = UUID.fromString(idArgument);
            Timber.d("Communication Unit ID: %s", communicationUnitId);
        }

        sblecHealthManager = new SblecHealthManager(communicationUnitId);
        gattHealthManager = new GattHealthManager(communicationUnitId);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.communication_unit_health, container, false);

        appBarLayout = getActivity().findViewById(R.id.toolbar_layout);

        sblecContainer = rootView.findViewById(R.id.sblecContainer);
        sblecDescriptionTextView = rootView.findViewById(R.id.sblecDescription);
        sblecIconImageView = rootView.findViewById(R.id.sblecIcon);
        sblecActiveDevicesTextView = rootView.findViewById(R.id.sblecActiveDevices);
        sblecOperationalChipsTextView = rootView.findViewById(R.id.sblecOperationalChips);

        gattContainer = rootView.findViewById(R.id.gattContainer);
        gattDescriptionTextView = rootView.findViewById(R.id.gattDescription);
        gattIconImageView = rootView.findViewById(R.id.gattIcon);
        gattActiveDevicesTextView = rootView.findViewById(R.id.gattActiveDevices);
        gattOperationalChipsTextView = rootView.findViewById(R.id.gattOperationalChips);

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
                monitorSblecHealth()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()),
                monitorGattHealth()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
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
                        .doOnError(throwable -> {
                            Timber.w(throwable, "SBLEC health check failed");
                            if (throwable instanceof AdvertiserException) {
                                indicateSblecUnknown();
                            } else {
                                indicateSblecUnhealthy(throwable);
                            }
                        })
                        .ignoreElement()
                        .onErrorComplete())
                .doFinally(this::indicateSblecUnknown);

        return sblecHealthManager.initialize(getContext())
                .andThen(monitor);
    }

    private void indicateSblecChecking() {
        Timber.d("indicateSblecChecking() called");
        sblecDescriptionTextView.setText(R.string.monitoring_checking);
        sblecIconImageView.setImageResource(R.drawable.detection_start);
    }

    private void indicateSblecHealthy(@NonNull HealthCheckResult healthCheckResult) {
        Timber.d("indicateSblecHealthy() called with: healthCheckResult = [%s]", healthCheckResult);
        sblecDescriptionTextView.setText(R.string.monitoring_healthy);
        sblecIconImageView.setImageResource(R.drawable.monitoring_healthy);
        sblecContainer.setBackgroundResource(R.color.monitoring_healthy);
        sblecActiveDevicesTextView.setText(String.valueOf(healthCheckResult.getActiveDevices()));
        sblecOperationalChipsTextView.setText(String.valueOf(healthCheckResult.getOperationalBluetoothChips()));
        trackHealth("SBLEC", true);
    }

    private void indicateSblecUnhealthy(@NonNull Throwable throwable) {
        Timber.w("indicateSblecUnhealthy() called with: throwable = [%s]", throwable);
        sblecDescriptionTextView.setText(R.string.monitoring_unhealthy);
        sblecIconImageView.setImageResource(R.drawable.monitoring_unhealthy);
        sblecContainer.setBackgroundResource(R.color.monitoring_unhealthy);
        sblecActiveDevicesTextView.setText(R.string.monitoring_count_unknown);
        sblecOperationalChipsTextView.setText(R.string.monitoring_count_unknown);
        trackHealth("SBLEC", false);
    }

    private void indicateSblecUnknown() {
        Timber.d("indicateSblecUnknown() called");
        sblecDescriptionTextView.setText(R.string.monitoring_unknown);
        sblecIconImageView.setImageResource(R.drawable.monitoring_unknown);
        sblecContainer.setBackgroundResource(R.color.monitoring_unknown);
        sblecActiveDevicesTextView.setText(R.string.monitoring_count_unknown);
        sblecOperationalChipsTextView.setText(R.string.monitoring_count_unknown);
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
                        .doOnError(throwable -> {
                            Timber.w(throwable, "GATT health check failed");
                            if (throwable instanceof AdvertiserException) {
                                indicateGattUnknown();
                            } else {
                                indicateGattUnhealthy(throwable);
                            }
                        })
                        .ignoreElement()
                        .onErrorComplete())
                .doFinally(this::indicateGattUnknown);

        return gattHealthManager.initialize(getContext())
                .andThen(monitor);
    }

    private void indicateGattChecking() {
        Timber.d("indicateGattChecking() called");
        gattDescriptionTextView.setText(R.string.monitoring_checking);
        gattIconImageView.setImageResource(R.drawable.detection_start);
    }

    private void indicateGattHealthy(@NonNull HealthCheckResult healthCheckResult) {
        Timber.d("indicateGattHealthy() called with: healthCheckResult = [%s]", healthCheckResult);
        gattDescriptionTextView.setText(R.string.monitoring_healthy);
        gattIconImageView.setImageResource(R.drawable.monitoring_healthy);
        gattContainer.setBackgroundResource(R.color.monitoring_healthy);
        gattActiveDevicesTextView.setText(String.valueOf(healthCheckResult.getActiveDevices()));
        gattOperationalChipsTextView.setText(String.valueOf(healthCheckResult.getOperationalBluetoothChips()));
        trackHealth("GATT", true);
    }

    private void indicateGattUnhealthy(@NonNull Throwable throwable) {
        Timber.w("indicateGattUnhealthy() called with: throwable = [%s]", throwable);
        gattDescriptionTextView.setText(R.string.monitoring_unhealthy);
        gattIconImageView.setImageResource(R.drawable.monitoring_unhealthy);
        gattContainer.setBackgroundResource(R.color.monitoring_unhealthy);
        gattActiveDevicesTextView.setText(R.string.monitoring_count_unknown);
        gattOperationalChipsTextView.setText(R.string.monitoring_count_unknown);
        trackHealth("GATT", false);
    }

    private void indicateGattUnknown() {
        Timber.d("indicateGattUnknown() called");
        gattDescriptionTextView.setText(R.string.monitoring_unknown);
        gattIconImageView.setImageResource(R.drawable.monitoring_unknown);
        gattContainer.setBackgroundResource(R.color.monitoring_unknown);
        gattActiveDevicesTextView.setText(R.string.monitoring_count_unknown);
        gattOperationalChipsTextView.setText(R.string.monitoring_count_unknown);
    }

    @SuppressLint("CheckResult")
    private void trackHealth(@NonNull String protocol, boolean healthy) {
        Completable sendToFeedbackServer = Completable.create(emitter -> {
            try {
                URL url = new URL("https://feedbackseamless.nexenio.com/api/feedback");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setDoOutput(true);
                connection.setDoInput(true);

                String id = protocol.toLowerCase() + "-" + (healthy ? "healthy" : "unhealthy") + "-" + communicationUnitId;
                String name = healthy ? "Healthy" : "Unhealthy";
                String path = "seamless-gate/" + id;

                JSONObject selectedOption = new JSONObject();
                selectedOption.put("id", id);
                selectedOption.put("name", name);
                selectedOption.put("path", path);

                JSONObject feedback = new JSONObject();
                feedback.put("sessionId", UUID.randomUUID());
                feedback.put("selectedOption", selectedOption);

                DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                os.writeBytes(feedback.toString());

                os.flush();
                os.close();

                Timber.v("Health tracking response: %s", connection.getResponseMessage());
                connection.disconnect();
                emitter.onComplete();
            } catch (Exception e) {
                emitter.tryOnError(e);
            }
        });

        sendToFeedbackServer.subscribeOn(Schedulers.io())
                .timeout(5, TimeUnit.SECONDS)
                .subscribe(
                        () -> Timber.v("%s health tracked", protocol),
                        throwable -> Timber.w(throwable, "Unable to track %s health", protocol)
                );
    }

}
