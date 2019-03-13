package com.nexenio.seamlessauthenticationintegrationsample;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.nexenio.seamlessauthentication.AuthenticationProperties;
import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;

import org.jetbrains.annotations.NotNull;

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

    private boolean seamlessAuthenticationEnabled = false;
    private double rangeThreshold = 0.5;

    private AuthenticationProperties authenticationProperties;

    private CollapsingToolbarLayout appBarLayout;
    private TextView nameTextView;
    private TextView idTextView;
    private TextView descriptionTextView;
    private MaterialButton authenticateButton;
    private SwitchCompat seamlessAuthenticationSwitch;
    private AppCompatSpinner rangeSpinner;

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

            private UUID userId = UUID.randomUUID();
            private UUID deviceId = UUID.randomUUID();

            @Override
            public Single<String> getUserName() {
                return Single.just("Demo User");
            }

            @Override
            public Single<UUID> getUserId() {
                return Single.just(userId);
            }

            @Override
            public Single<UUID> getDeviceId() {
                return Single.just(deviceId);
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
        seamlessAuthenticationSwitch = rootView.findViewById(R.id.seamlessAuthenticationSwitch);
        rangeSpinner = rootView.findViewById(R.id.rangeSpinner);
        authenticateButton = rootView.findViewById(R.id.authenticateButton);

        authenticateButton.setOnClickListener(v -> authenticate());

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

    private void authenticate() {
        Timber.d("authenticate() called");
        if (authenticator == null) {
            return;
        }
        if (authenticationDisposable != null && !authenticationDisposable.isDisposed()) {
            return;
        }
        authenticationDisposable = authenticator.authenticate(authenticationProperties)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        () -> Timber.i("Authentication succeeded"),
                        throwable -> Timber.w(throwable, "Authentication failed")
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
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showAuthenticator);
    }

    private void stopUpdatingAuthenticator() {
        Timber.d("stopUpdatingAuthenticator() called");
        if (authenticatorUpdateDisposable != null && !authenticatorUpdateDisposable.isDisposed()) {
            authenticatorUpdateDisposable.dispose();
        }
    }

    private void showAuthenticator(@NonNull SeamlessAuthenticator authenticator) {
        Timber.d("Seamless authenticator detected: %s", authenticator);

        this.authenticator = authenticator;

        if (seamlessAuthenticationEnabled && authenticator.getDistance().blockingGet() <= rangeThreshold) {
            authenticate();
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
    }

}
