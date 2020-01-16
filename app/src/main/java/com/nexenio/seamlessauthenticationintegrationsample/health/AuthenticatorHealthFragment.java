package com.nexenio.seamlessauthenticationintegrationsample.health;

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
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SampleApplication;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorListActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorViewHolder;
import com.nexenio.seamlessauthenticationintegrationsample.visualization.GateView;

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
 * A fragment representing a single Authenticator health check screen. This fragment is either
 * contained in a {@link AuthenticatorListActivity} in two-pane mode (on tablets) or a {@link
 * AuthenticatorHealthActivity} on handsets.
 */
public class AuthenticatorHealthFragment extends Fragment {

    /**
     * The fragment argument representing the item ID that this fragment represents.
     */
    public static final String KEY_AUTHENTICATOR_ID = "authenticator_id";

    private UUID authenticatorId;

    private SeamlessAuthenticatorDetector authenticatorDetector;
    private Disposable authenticatorUpdateDisposable;

    private SeamlessAuthenticator authenticator;

    private AuthenticationProperties authenticationProperties;

    private CollapsingToolbarLayout appBarLayout;
    private TextView nameTextView;
    private TextView idTextView;
    private TextView descriptionTextView;
    private GateView gateView;
    private TextView authenticationIdsTextView;
    private MaterialButton authenticateButton;
    private SwitchCompat seamlessAuthenticationSwitch;
    private AppCompatSpinner rangeSpinner;

    private TextView beaconDescriptionTextView;

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
