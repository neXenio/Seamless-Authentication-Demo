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

import com.nexenio.seamlessauthentication.AuthenticationProperties;
import com.nexenio.seamlessauthentication.CommunicationUnit;
import com.nexenio.seamlessauthentication.CommunicationUnitDetector;
import com.nexenio.seamlessauthentication.accesscontrol.gate.Gate;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.GatewayDirection;
import com.nexenio.seamlessauthentication.distance.DistanceProvider;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.lock.DirectionLockBeacon;
import com.nexenio.seamlessauthentication.internal.beacon.CommunicationUnitAdvertisingPacket;
import com.nexenio.seamlessauthentication.internal.beacon.CommunicationUnitBeacon;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SampleApplication;
import com.nexenio.seamlessauthenticationintegrationsample.health.CommunicationUnitHealthActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.CommunicationUnitListActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.CommunicationUnitViewHolder;
import com.nexenio.seamlessauthenticationintegrationsample.visualization.GateView;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
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
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.seamless.sblec.positioning.beacon.AdvertisingPacket;
import me.seamless.sblec.positioning.beacon.Beacon;
import me.seamless.sblec.positioning.internal.beacon.BaseAdvertisingPacket;
import timber.log.Timber;

/**
 * A fragment representing a single {@link CommunicationUnit} detail screen. This fragment is either
 * contained in a {@link CommunicationUnitListActivity} in two-pane mode (on tablets) or a {@link
 * CommunicationUnitDetailActivity} on handsets.
 */
public class CommunicationUnitDetailFragment extends Fragment {

    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String KEY_COMMUNICATION_UNIT_ID = "communication_unit_id";

    private UUID communicationUnitId;

    private CommunicationUnitDetector communicationUnitDetector;
    private Disposable communicationUnitUpdateDisposable;

    private CommunicationUnit communicationUnit;
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
    public CommunicationUnitDetailFragment() {
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

        authenticationProperties = new AuthenticationProperties() {

            @Override
            public Single<UUID> getUserId() {
                return Single.just(application.getUserId());
            }

            @Override
            public Single<UUID> getDeviceId() {
                return Single.just(application.getDeviceId());
            }

            @Override
            public Maybe<byte[]> getAdditionalData() {
                return Maybe.fromCallable(() -> "Open Sesame!".getBytes(StandardCharsets.UTF_8));
            }

        };
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.communication_unit_detail, container, false);

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

        authenticateButton.setOnClickListener(v -> authenticate(communicationUnit));
        monitorHealthButton.setOnClickListener(v -> monitorHealth(communicationUnit));

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
        startUpdatingCommunicationUnit();
    }

    @Override
    public void onDetach() {
        stopUpdatingCommunicationUnit();
        super.onDetach();
    }

    private void monitorHealth(@NonNull CommunicationUnit communicationUnit) {
        Timber.d("monitorHealth() called with: communicationUnit = [%s]", communicationUnit);
        Intent intent = new Intent(getContext(), CommunicationUnitHealthActivity.class);
        intent.putExtra(CommunicationUnitDetailFragment.KEY_COMMUNICATION_UNIT_ID, communicationUnit.getId().blockingGet().toString());
        getContext().startActivity(intent);
    }

    private void anticipateAuthentication(@NonNull CommunicationUnit communicationUnit) {
        Timber.d("anticipateAuthentication() called with: communicationUnit = [%s]", communicationUnit);
        if (anticipateAuthenticationDisposable != null && !anticipateAuthenticationDisposable.isDisposed()) {
            return;
        }
        anticipateAuthenticationDisposable = communicationUnit.anticipateAuthentication(authenticationProperties)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> Timber.i("Authentication anticipation succeeded"),
                        throwable -> Timber.w(throwable, "Authentication anticipation failed")
                );
    }

    private void authenticate(@NonNull CommunicationUnit communicationUnit) {
        Timber.d("authenticate() called with: communicationUnit = [%s]", communicationUnit);
        if (authenticationDisposable != null && !authenticationDisposable.isDisposed()) {
            return;
        }
        authenticationDisposable = communicationUnit.authenticate(authenticationProperties)
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

    private void startUpdatingCommunicationUnit() {
        Timber.d("startUpdatingCommunicationUnit() called");
        communicationUnitUpdateDisposable = Flowable.interval(1, TimeUnit.SECONDS)
                .flatMapMaybe(count -> communicationUnitDetector.getCurrentlyDetectedCommunicationUnits()
                        .filter(communicationUnit -> communicationUnit.getId()
                                .map(uuid -> uuid.equals(communicationUnitId))
                                .onErrorReturnItem(false)
                                .blockingGet())
                        .firstElement())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::showCommunicationUnit,
                        throwable -> Timber.w(throwable, "Unable to update communication unit")
                );
    }

    private void stopUpdatingCommunicationUnit() {
        Timber.d("stopUpdatingCommunicationUnit() called");
        if (communicationUnitUpdateDisposable != null && !communicationUnitUpdateDisposable.isDisposed()) {
            communicationUnitUpdateDisposable.dispose();
        }
    }

    private void showCommunicationUnit(@NonNull CommunicationUnit communicationUnit) {
        if (this.communicationUnit != communicationUnit) {
            Timber.d("Communication Unit updated: %s", communicationUnit);
            this.communicationUnit = communicationUnit;
            anticipateAuthentication(communicationUnit);
        }

        double distance = communicationUnit.getDistanceProvider()
                .flatMap(DistanceProvider::getDistance)
                .blockingGet();

        if (seamlessAuthenticationEnabled && distance <= rangeThreshold) {
            authenticate(communicationUnit);
        }

        String name = CommunicationUnitViewHolder.getReadableName(communicationUnit, getContext()).blockingGet();
        String id = CommunicationUnitViewHolder.getReadableId(communicationUnit, getContext()).blockingGet();
        String description = CommunicationUnitViewHolder.getReadableDescription(communicationUnit, getContext()).blockingGet();

        if (appBarLayout != null) {
            appBarLayout.setTitle(name);
        }

        nameTextView.setText(name);
        idTextView.setText(id);
        descriptionTextView.setText(description);

        if (communicationUnit instanceof Gate) {
            Gate gate = (Gate) communicationUnit;
            List<CommunicationUnitBeacon<? extends CommunicationUnitAdvertisingPacket>> beacons = new ArrayList<>();

            beacons.addAll(gate.getDetectionBeacons()
                    .toList().blockingGet());

            beacons.addAll(gate.getDirectionLockBeacons()
                    .toList().blockingGet());

            StringBuilder beaconDescription = new StringBuilder();
            for (CommunicationUnitBeacon<? extends CommunicationUnitAdvertisingPacket> beacon : beacons) {
                beaconDescription.append(getReadableDescription(beacon, getContext()))
                        .append("\n\n");
            }
            beaconDescriptionTextView.setText(beaconDescription.toString());

            gateView.onCommunicationUnitUpdated(gate);
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

    public static String getReadableDescription(@NonNull CommunicationUnitBeacon<? extends CommunicationUnitAdvertisingPacket> beacon, @NonNull Context context) {
        StringBuilder description = new StringBuilder();

        // TODO: 04.05.20 refactor

        String type = getReadableBeaconType(beacon, context);
        String mac = beacon.getMacAddress().blockingGet();
        //String tx = String.valueOf(beacon.getTran());
        String tx = "?";
        String rssi = beacon.getLastAdvertisingPacket()
                .flatMapSingle(BaseAdvertisingPacket::getRssi)
                .map(String::valueOf)
                .onErrorReturnItem("?")
                .blockingGet();
        String calibratedRssi = String.valueOf(beacon.getCalibratedRssi().blockingGet());
        //String distance = String.format(Locale.US, "%.2f", beacon.getDistance());
        String distance = "?";
        String interval = getReadableBeaconAdvertisingInterval(beacon, context);

        description.append(context.getString(R.string.generic_beacon_description,
                type, mac, tx, rssi, calibratedRssi, distance, interval));

        if (beacon instanceof GatewayDetectionBeacon) {
            GatewayDetectionBeacon gatewayDetectionBeacon = (GatewayDetectionBeacon) beacon;
            String gate = String.valueOf(gatewayDetectionBeacon.getCommunicationUnitIndex().blockingGet());
            String gateway = String.valueOf(gatewayDetectionBeacon.getGatewayIndex().blockingGet());
            String direction = getReadableBeaconDirection(gatewayDetectionBeacon.getOpeningDirection().blockingGet(), context);
            String position = getReadableBeaconPosition(gatewayDetectionBeacon, context);

            description.append("\n").append(context.getString(R.string.gateway_detection_beacon_description,
                    gate, gateway, direction, position));
        } else if (beacon instanceof DirectionLockBeacon) {
            DirectionLockBeacon directionLockBeacon = (DirectionLockBeacon) beacon;
            String gate = String.valueOf(directionLockBeacon.getCommunicationUnitIndex().blockingGet());
            String direction = getReadableBeaconDirection(directionLockBeacon.getOpeningDirection().blockingGet(), context);

            description.append("\n").append(context.getString(R.string.gateway_direction_lock_beacon_description,
                    gate, direction));
        }

        return description.toString();
    }

    public static String getReadableBeaconAdvertisingInterval(@NonNull Beacon<? extends AdvertisingPacket> beacon, @NonNull Context context) {
        try {
            double hertz = beacon.getAdvertisingFrequency().blockingGet();
            return String.format(Locale.US, "%.2f", hertz);
        } catch (Exception e) {
            return context.getString(R.string.unknown);
        }
    }

    public static String getReadableBeaconType(@NonNull Beacon beacon, @NonNull Context context) {
        if (beacon instanceof GatewayDetectionBeacon) {
            return context.getString(R.string.beacon_type_gateway_detection);
        } else if (beacon instanceof DirectionLockBeacon) {
            return context.getString(R.string.beacon_type_direction_lock);
        } else {
            return context.getString(R.string.beacon_type_unknown);
        }
    }

    public static String getReadableBeaconPosition(@NonNull GatewayDetectionBeacon beacon, @NonNull Context context) {
        switch (beacon.getPosition().blockingGet()) {
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
