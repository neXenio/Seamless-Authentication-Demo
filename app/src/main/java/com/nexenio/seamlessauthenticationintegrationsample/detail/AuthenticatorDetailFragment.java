package com.nexenio.seamlessauthenticationintegrationsample.detail;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.nexenio.bleindoorpositioning.ble.beacon.Beacon;
import com.nexenio.seamlessauthentication.AuthenticationProperties;
import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;
import com.nexenio.seamlessauthentication.accesscontrol.gate.Gate;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.GatewayDirection;
import com.nexenio.seamlessauthentication.distance.DistanceProvider;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.lock.GatewayDirectionLockBeacon;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SampleApplication;
import com.nexenio.seamlessauthenticationintegrationsample.health.AuthenticatorHealthActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorListActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorViewHolder;
import com.nexenio.seamlessauthenticationintegrationsample.visualization.GateView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * A fragment representing a single Authenticator detail screen. This fragment is either contained
 * in a {@link AuthenticatorListActivity} in two-pane mode (on tablets) or a {@link
 * AuthenticatorDetailActivity} on handsets.
 */
public class AuthenticatorDetailFragment extends Fragment {

    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String KEY_AUTHENTICATOR_ID = "authenticator_id";

    private UUID authenticatorId;

    private SeamlessAuthenticatorDetector authenticatorDetector;
    private Disposable authenticatorUpdateDisposable;

    private SeamlessAuthenticator authenticator;
    private Disposable authenticationDisposable;
    private Disposable anticipateAuthenticationDisposable;

    private boolean seamlessAuthenticationEnabled = false;
    private double rangeThreshold = 0.5;

    private AuthenticationProperties authenticationProperties;

    private CollapsingToolbarLayout appBarLayout;
    private TextView nameTextView;
    private TextView idTextView;
    private TextView descriptionTextView;
    private GateView gateView;
    private TextView authenticationIdsTextView;
    private MaterialButton authenticateButton;
    private MaterialButton monitorHealthButton;
    private SwitchCompat seamlessAuthenticationSwitch;
    private AppCompatSpinner rangeSpinner;

    private TextView beaconDescriptionTextView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
     * screen orientation changes).
     */
    public AuthenticatorDetailFragment() {
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

        authenticationProperties = new AuthenticationProperties() {

            @Override
            public Single<String> getUserName() {
                return Single.just("Demo User");
            }

            @Override
            public Single<UUID> getUserId() {
                return Single.just(application.getUserId());
            }

            @Override
            public Single<UUID> getDeviceId() {
                return Single.just(application.getDeviceId());
            }
        };
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.authenticator_detail, container, false);

        appBarLayout = getActivity().findViewById(R.id.toolbar_layout);
        idTextView = rootView.findViewById(R.id.idTextView);
        nameTextView = rootView.findViewById(R.id.nameTextView);
        descriptionTextView = rootView.findViewById(R.id.descriptionTextView);
        gateView = rootView.findViewById(R.id.visualizationView);
        seamlessAuthenticationSwitch = rootView.findViewById(R.id.seamlessAuthenticationSwitch);
        rangeSpinner = rootView.findViewById(R.id.rangeSpinner);
        authenticationIdsTextView = rootView.findViewById(R.id.authenticationIdsTextView);
        authenticateButton = rootView.findViewById(R.id.authenticateButton);
        monitorHealthButton = rootView.findViewById(R.id.monitorHealthButton);

        authenticateButton.setOnClickListener(v -> authenticate(authenticator));
        monitorHealthButton.setOnClickListener(v -> monitorHealth(authenticator));

        seamlessAuthenticationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            seamlessAuthenticationEnabled = isChecked;
        });

        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        rangeThreshold = 0.5;
                        break;
                    case 1:
                        rangeThreshold = 1;
                        break;
                    case 2:
                        rangeThreshold = 2;
                        break;
                    case 3:
                        rangeThreshold = 5;
                        break;
                    case 4:
                        rangeThreshold = 10;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        authenticationIdsTextView.setText(getContext().getString(
                R.string.description_authentication_ids,
                authenticationProperties.getUserId().blockingGet().toString(),
                authenticationProperties.getDeviceId().blockingGet().toString()
        ));

        beaconDescriptionTextView = rootView.findViewById(R.id.beaconDescriptionTextView);

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        startUpdatingAuthenticator();
    }

    @Override
    public void onDetach() {
        stopUpdatingAuthenticator();
        super.onDetach();
    }

    private void monitorHealth(@NonNull SeamlessAuthenticator authenticator) {
        Timber.d("monitorHealth() called with: authenticator = [%s]", authenticator);
        Intent intent = new Intent(getContext(), AuthenticatorHealthActivity.class);
        intent.putExtra(AuthenticatorDetailFragment.KEY_AUTHENTICATOR_ID, authenticator.getId().blockingGet().toString());
        getContext().startActivity(intent);
    }

    private void anticipateAuthentication(@NonNull SeamlessAuthenticator authenticator) {
        Timber.d("anticipateAuthentication() called");
        if (anticipateAuthenticationDisposable != null && !anticipateAuthenticationDisposable.isDisposed()) {
            return;
        }
        anticipateAuthenticationDisposable = authenticator.anticipateAuthentication(authenticationProperties)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.i("Authentication anticipation succeeded"),
                        throwable -> Timber.w(throwable, "Authentication anticipation failed")
                );
    }

    private void authenticate(@NonNull SeamlessAuthenticator authenticator) {
        Timber.d("authenticate() called");
        if (authenticationDisposable != null && !authenticationDisposable.isDisposed()) {
            return;
        }
        authenticationDisposable = authenticator.authenticate(authenticationProperties)
                .doOnSubscribe(disposable -> vibrate(200))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Timber.i("Authentication succeeded");
                            Toast.makeText(getContext(), R.string.status_authentication_succeeded, Toast.LENGTH_SHORT).show();
                            vibrate(200);
                        },
                        throwable -> {
                            Timber.w(throwable, "Authentication failed");
                            Toast.makeText(getContext(), R.string.status_authentication_failed, Toast.LENGTH_SHORT).show();
                            vibrate(400);
                        }
                );
    }

    private void startUpdatingAuthenticator() {
        Timber.d("startUpdatingAuthenticator() called");
        authenticatorUpdateDisposable = Flowable.interval(1, TimeUnit.SECONDS)
                .flatMapMaybe(count -> authenticatorDetector.getDetectedAuthenticators()
                        .filter(authenticator -> authenticator.getId()
                                .map(uuid -> uuid.equals(authenticatorId))
                                .onErrorReturnItem(false)
                                .blockingGet())
                        .firstElement())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::showAuthenticator,
                        throwable -> Timber.w(throwable, "Unable to update authenticator")
                );
    }

    private void stopUpdatingAuthenticator() {
        Timber.d("stopUpdatingAuthenticator() called");
        if (authenticatorUpdateDisposable != null && !authenticatorUpdateDisposable.isDisposed()) {
            authenticatorUpdateDisposable.dispose();
        }
    }

    private void showAuthenticator(@NonNull SeamlessAuthenticator authenticator) {
        if (this.authenticator != authenticator) {
            Timber.d("Authenticator updated: %s", authenticator);
            this.authenticator = authenticator;
            anticipateAuthentication(authenticator);
        }

        double distance = authenticator.getDistanceProvider()
                .flatMap(DistanceProvider::getDistance)
                .blockingGet();

        if (seamlessAuthenticationEnabled && distance <= rangeThreshold) {
            authenticate(authenticator);
        }

        String name = AuthenticatorViewHolder.getReadableName(authenticator, getContext()).blockingGet();
        String id = AuthenticatorViewHolder.getReadableId(authenticator, getContext()).blockingGet();
        String description = AuthenticatorViewHolder.getReadableDescription(authenticator, getContext()).blockingGet();

        if (appBarLayout != null) {
            appBarLayout.setTitle(name);
        }

        nameTextView.setText(name);
        idTextView.setText(id);
        descriptionTextView.setText(description);

        if (authenticator instanceof Gate) {
            Gate gate = (Gate) authenticator;
            List<Beacon> beacons = new ArrayList<>();

            beacons.addAll(gate.getDetectionBeacons()
                    .toList().blockingGet());

            beacons.addAll(gate.getDirectionLockBeacons()
                    .toList().blockingGet());

            StringBuilder beaconDescription = new StringBuilder();
            for (Beacon beacon : beacons) {
                beaconDescription.append(getReadableDescription(beacon, getContext()))
                        .append("\n\n");
            }
            beaconDescriptionTextView.setText(beaconDescription.toString());

            gateView.onAuthenticatorUpdated(gate);
        }
    }

    private void vibrate(long duration) {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(duration);
        }
    }

    public static String getReadableDescription(@NonNull Beacon beacon, @NonNull Context context) {
        StringBuilder description = new StringBuilder();

        String type = getReadableBeaconType(beacon, context);
        String mac = beacon.getMacAddress();
        String tx = String.valueOf(beacon.getTransmissionPower());
        String rssi = String.valueOf(Math.round(beacon.getFilteredRssi()));
        String calibratedRssi = String.valueOf(beacon.getCalibratedRssi());
        String distance = String.format(Locale.US, "%.2f", beacon.getDistance());
        String interval = getReadableBeaconAdvertisingInterval(beacon, context);

        description.append(context.getString(R.string.generic_beacon_description,
                type, mac, tx, rssi, calibratedRssi, distance, interval));

        if (beacon instanceof GatewayDetectionBeacon) {
            GatewayDetectionBeacon gatewayDetectionBeacon = (GatewayDetectionBeacon) beacon;
            String gate = String.valueOf(gatewayDetectionBeacon.getGateIndex());
            String gateway = String.valueOf(gatewayDetectionBeacon.getGatewayIndex());
            String direction = getReadableBeaconDirection(gatewayDetectionBeacon.getGatewayDirection(), context);
            String position = getReadableBeaconPosition(gatewayDetectionBeacon, context);

            description.append("\n").append(context.getString(R.string.gateway_detection_beacon_description,
                    gate, gateway, direction, position));
        } else if (beacon instanceof GatewayDirectionLockBeacon) {
            GatewayDirectionLockBeacon directionLockBeacon = (GatewayDirectionLockBeacon) beacon;
            String gate = String.valueOf(directionLockBeacon.getGateIndex());
            String direction = getReadableBeaconDirection(directionLockBeacon.getGatewayDirection(), context);

            description.append("\n").append(context.getString(R.string.gateway_direction_lock_beacon_description,
                    gate, direction));
        }

        return description.toString();
    }

    public static String getReadableBeaconAdvertisingInterval(@NonNull Beacon beacon, @NonNull Context context) {
        try {
            long oldestTimestamp = beacon.getOldestAdvertisingPacket().getTimestamp();
            long latestTimestamp = beacon.getLatestAdvertisingPacket().getTimestamp();
            double durationInSeconds = (double) (latestTimestamp - oldestTimestamp) / 1000;

            double hertz = 0;
            if (durationInSeconds != 0) {
                int count = beacon.getAdvertisingPackets().size();
                hertz = count / durationInSeconds;
            }
            return String.format(Locale.US, "%.2f", hertz);
        } catch (Exception e) {
            return context.getString(R.string.unknown);
        }
    }

    public static String getReadableBeaconType(@NonNull Beacon beacon, @NonNull Context context) {
        if (beacon instanceof GatewayDetectionBeacon) {
            return context.getString(R.string.beacon_type_gateway_detection);
        } else if (beacon instanceof GatewayDirectionLockBeacon) {
            return context.getString(R.string.beacon_type_direction_lock);
        } else {
            return context.getString(R.string.beacon_type_unknown);
        }
    }

    public static String getReadableBeaconPosition(@NonNull GatewayDetectionBeacon beacon, @NonNull Context context) {
        switch (beacon.getPosition()) {
            case GatewayDetectionBeacon.LEFT: {
                return context.getString(R.string.beacon_position_left);
            }
            case GatewayDetectionBeacon.RIGHT: {
                return context.getString(R.string.beacon_position_right);
            }
            default: {
                return context.getString(R.string.unknown);
            }
        }
    }

    public static String getReadableBeaconDirection(@GatewayDirection.Direction int direction, @NonNull Context context) {
        switch (direction) {
            case GatewayDirection.ENTRY: {
                return context.getString(R.string.beacon_direction_exit);
            }
            case GatewayDirection.EXIT: {
                return context.getString(R.string.beacon_direction_entry);
            }
            default: {
                return context.getString(R.string.unknown);
            }
        }
    }

}
