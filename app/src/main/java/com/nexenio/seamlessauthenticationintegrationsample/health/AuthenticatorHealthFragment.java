package com.nexenio.seamlessauthenticationintegrationsample.health;

import com.google.android.material.appbar.CollapsingToolbarLayout;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nexenio.sblec.Sblec;
import com.nexenio.sblec.payload.PayloadIdFilter;
import com.nexenio.sblec.payload.PayloadWrapper;
import com.nexenio.sblec.receiver.CompletelyReceivedFilter;
import com.nexenio.sblec.receiver.PayloadReceiver;
import com.nexenio.sblec.sender.PayloadSender;
import com.nexenio.sblec.sender.SenderPayload;
import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.SeamlessAuthenticatorDetector;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SampleApplication;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorListActivity;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
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
    private static final long HEALTH_CHECK_TIMEOUT = TimeUnit.SECONDS.toMillis(5);

    protected Sblec sblec;
    protected int deviceIdHashCode;
    protected PayloadSender sender;
    protected PayloadReceiver receiver;

    private UUID authenticatorId;

    private SeamlessAuthenticatorDetector authenticatorDetector;
    private Disposable healthMonitorDisposable;

    private SeamlessAuthenticator authenticator;

    private CollapsingToolbarLayout appBarLayout;
    private TextView sblecDescriptionTextView;
    private TextView gattDescriptionTextView;
    private ImageView sblecIconImageView;
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

        Context context = getContext();
        this.sblec = Sblec.getInstance();
        this.deviceIdHashCode = Sblec.getDeviceIdHashCode(context);
        this.sender = sblec.getOrCreatePayloadSender(context, Sblec.COMPANY_ID_NEXENIO);
        this.receiver = sblec.getOrCreatePayloadReceiver(context, Sblec.COMPANY_ID_NEXENIO);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.authenticator_health, container, false);

        appBarLayout = getActivity().findViewById(R.id.toolbar_layout);
        sblecDescriptionTextView = rootView.findViewById(R.id.sblecDescription);
        gattDescriptionTextView = rootView.findViewById(R.id.gattDescription);
        sblecIconImageView = rootView.findViewById(R.id.sblecIcon);
        gattIconImageView = rootView.findViewById(R.id.gattIcon);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
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
        return Observable.interval(1, HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .doOnNext(count -> {
                    Timber.d("Initiating SBLEC health check # %d", count + 1);
                    indicateSblecChecking();
                })
                .flatMapCompletable(count -> getSblecHealthCheckResult()
                        .doOnSuccess(this::indicateSblecHealthy)
                        .doOnError(this::indicateSblecUnhealthy)
                        .ignoreElement()
                        .onErrorComplete())
                .doFinally(this::indicateSblecUnknown);
    }

    private Single<HealthCheckResult> getSblecHealthCheckResult() {
        return Single.create(emitter -> {
            Disposable sendDisposable = sendSblecHealthCheckRequest()
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> Timber.v("SBLEC health request sending completed"),
                            emitter::onError
                    );

            Disposable receiveDisposable = receiveSblecHealthCheckResponseHack()
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            emitter::onSuccess,
                            emitter::onError
                    );

            emitter.setDisposable(new CompositeDisposable(sendDisposable, receiveDisposable));
        });
    }

    private Single<SenderPayload> createSblecHealthCheckRequest() {
        return Single.defer(() -> new HealthCheckRequestPayloadWrapper.Builder()
                .setAuthenticatorId(authenticatorId)
                .build()
                .toSenderPayload());
    }

    private Single<SenderPayload> createSblecHealthCheckRequestHack() {
        return Single.defer(() -> (new PayloadWrapper() {
            @Override
            public int getId() {
                return 11;
            }

            @Override
            public Completable readFromBuffer(@NonNull ByteBuffer byteBuffer) {
                return Completable.complete();
            }

            @Override
            public Single<ByteBuffer> writeToBuffer() {
                return Single.just(ByteBuffer.wrap(new byte[]{1, 2, 17, 15}));
            }
        }).toSenderPayload());
    }

    private Completable sendSblecHealthCheckRequest() {
        return createSblecHealthCheckRequestHack()
                .flatMapCompletable(senderPayload -> sender.send(senderPayload)
                        .timeout(3, TimeUnit.SECONDS)
                        .onErrorResumeNext(throwable -> {
                            if (throwable instanceof TimeoutException) {
                                // don't treat the timeout as error,
                                // we want to stop it with intention
                                return Completable.complete();
                            } else {
                                return Completable.error(throwable);
                            }
                        }));
    }

    private Single<HealthCheckResult> receiveSblecHealthCheckResponse() {
        // TODO: 2020-01-17 filter for nonce
        return receiver.receive()
                .filter(new PayloadIdFilter(HealthCheckResponsePayloadWrapper.ID))
                .filter(new CompletelyReceivedFilter())
                .map(HealthCheckResponsePayloadWrapper::new)
                .filter(responsePayloadWrapper -> responsePayloadWrapper.getDeviceIdHashcode() == deviceIdHashCode)
                .map(HealthCheckResponsePayloadWrapper::getHealthCheckResult)
                .firstOrError()
                .timeout(HEALTH_CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private Single<HealthCheckResult> receiveSblecHealthCheckResponseHack() {
        return receiver.receive()
                .filter(new PayloadIdFilter(60))
                .filter(new CompletelyReceivedFilter())
                .map(receiverPayload -> new HealthCheckResult())
                .firstOrError()
                .timeout(HEALTH_CHECK_TIMEOUT, TimeUnit.MILLISECONDS);
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
    }

    private void indicateSblecUnhealthy(@NonNull Throwable throwable) {
        Timber.w("indicateSblecUnhealthy() called with: throwable = [%s]", throwable);
        sblecDescriptionTextView.setText(R.string.monitoring_unhealthy);
        sblecIconImageView.setImageResource(R.drawable.ic_close_black_24dp);
    }

    private void indicateSblecUnknown() {
        Timber.d("indicateSblecUnknown() called");
        sblecDescriptionTextView.setText(R.string.monitoring_unknown);
        sblecIconImageView.setImageResource(R.drawable.ic_sync_problem_black_24dp);
    }

    /*
        GATT Monitoring
     */

    private Completable monitorGattHealth() {
        return Observable.interval(HEALTH_CHECK_TIMEOUT, HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS, Schedulers.io())
                .doOnNext(count -> {
                    Timber.d("Initiating GATT health check # %d", count + 1);
                    indicateGattChecking();
                })
                .flatMapCompletable(count -> getGattHealthCheckResult()
                        .doOnSuccess(this::indicateGattHealthy)
                        .doOnError(this::indicateGattUnhealthy)
                        .ignoreElement()
                        .onErrorComplete())
                .doFinally(this::indicateGattUnknown);
    }

    private Single<HealthCheckResult> getGattHealthCheckResult() {
        return Single.error(new Throwable("GATT health check not implemented"));
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
    }

    private void indicateGattUnhealthy(@NonNull Throwable throwable) {
        Timber.w("indicateGattUnhealthy() called with: throwable = [%s]", throwable);
        gattDescriptionTextView.setText(R.string.monitoring_unhealthy);
        gattIconImageView.setImageResource(R.drawable.ic_close_black_24dp);
    }

    private void indicateGattUnknown() {
        Timber.d("indicateGattUnknown() called");
        gattDescriptionTextView.setText(R.string.monitoring_unknown);
        gattIconImageView.setImageResource(R.drawable.ic_sync_problem_black_24dp);
    }

}
