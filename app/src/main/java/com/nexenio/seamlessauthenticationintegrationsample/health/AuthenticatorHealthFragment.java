package com.nexenio.seamlessauthenticationintegrationsample.health;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SampleApplication;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorListActivity;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import io.reactivex.Completable;
import io.reactivex.Single;
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

    private static final long HEALTH_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    private UUID authenticatorId;

    private SeamlessAuthenticatorDetector authenticatorDetector;
    private Disposable healthMonitorDisposable;

    private SeamlessAuthenticator authenticator;

    private CollapsingToolbarLayout appBarLayout;
    private TextView sblecDescriptionTextView;
    private TextView gattDescriptionTextView;

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

        indicateSblecUnknown();
        indicateGattUnknown();
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.authenticator_health, container, false);

        appBarLayout = getActivity().findViewById(R.id.toolbar_layout);
        sblecDescriptionTextView = rootView.findViewById(R.id.sblecDescription);
        gattDescriptionTextView = rootView.findViewById(R.id.gattDescription);

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        startHealthMonitoring();
    }

    @Override
    public void onDetach() {
        stopHealthMonitoring();
        super.onDetach();
    }

    private void startHealthMonitoring() {
        Timber.d("startHealthMonitoring() called");

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
        return Completable.never();
    }

    private Single<HealthCheckResult> getSblecHealthCheckResult() {
        return Single.error(new Throwable("Not implemented"));
    }

    private void indicateSblecHealthy() {
        sblecDescriptionTextView.setText(R.string.monitoring_healthy);
    }

    private void indicateSblecUnhealthy() {
        sblecDescriptionTextView.setText(R.string.monitoring_unhealthy);
    }

    private void indicateSblecUnknown() {
        sblecDescriptionTextView.setText(R.string.monitoring_unknown);
    }

    /*
        GATT Monitoring
     */

    private Completable monitorGattHealth() {
        return Completable.never();
    }

    private Single<HealthCheckResult> getGattHealthCheckResult() {
        return Single.error(new Throwable("Not implemented"));
    }

    private void indicateGattHealthy() {
        gattDescriptionTextView.setText(R.string.monitoring_healthy);
    }

    private void indicateGattUnhealthy() {
        gattDescriptionTextView.setText(R.string.monitoring_unhealthy);
    }

    private void indicateGattUnknown() {
        gattDescriptionTextView.setText(R.string.monitoring_unknown);
    }

}
