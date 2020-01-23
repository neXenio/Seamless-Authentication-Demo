package com.nexenio.seamlessauthenticationintegrationsample.health.sblec;

import android.content.Context;

import com.nexenio.sblec.PerformanceModes;
import com.nexenio.sblec.Sblec;
import com.nexenio.sblec.payload.PayloadIdFilter;
import com.nexenio.sblec.receiver.CompletelyReceivedFilter;
import com.nexenio.sblec.receiver.PayloadReceiver;
import com.nexenio.sblec.sender.PayloadSender;
import com.nexenio.sblec.sender.SenderPayload;
import com.nexenio.seamlessauthenticationintegrationsample.health.HealthCheckResult;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SblecHealthManager {

    private final UUID authenticatorId;

    private Sblec sblec;
    private int deviceIdHashCode;
    private PayloadSender sender;
    private PayloadReceiver receiver;

    public SblecHealthManager(UUID authenticatorId) {
        this.authenticatorId = authenticatorId;
    }

    public Completable initialize(@NonNull Context context) {
        return Completable.fromAction(() -> {
            sblec = Sblec.getInstance();
            deviceIdHashCode = (short) Sblec.getDeviceIdHashCode(context);
            sender = sblec.createPayloadSender(context, Sblec.COMPANY_ID_NEXENIO);
            receiver = sblec.createPayloadReceiver(context, Sblec.COMPANY_ID_NEXENIO);
        }).andThen(Completable.defer(() -> Completable.mergeArray(
                sender.setPerformanceMode(PerformanceModes.LOW_LATENCY),
                receiver.setPerformanceMode(PerformanceModes.LOW_LATENCY)
        )));
    }

    public Single<HealthCheckResult> getHealthCheckResult() {
        return Single.create(emitter -> {
            Disposable sendDisposable = sendSblecHealthCheckRequest()
                    .doOnSubscribe(disposable -> Timber.d("SBLEC health request sending started"))
                    .doFinally(() -> Timber.d("SBLEC health request sending stopped"))
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {
                            },
                            emitter::onError
                    );

            Disposable receiveDisposable = receiveSblecHealthCheckResponse()
                    .doOnSubscribe(disposable -> Timber.d("SBLEC health request receiving started"))
                    .doFinally(() -> Timber.d("SBLEC health request receiving stopped"))
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

    private Completable sendSblecHealthCheckRequest() {
        return createSblecHealthCheckRequest()
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
        // TODO: 2020-01-22 filter for own device ID hashcode
        // TODO: 2020-01-17 filter for expected nonce
        return receiver.receive()
                .filter(new PayloadIdFilter(HealthCheckResponsePayloadWrapper.ID))
                .filter(new CompletelyReceivedFilter())
                .doOnNext(receiverPayload -> Timber.v("Received health check response: %s", receiverPayload))
                .map(HealthCheckResponsePayloadWrapper::new)
                .doOnNext(healthCheckResponsePayloadWrapper -> Timber.i("Received health check response: %s", healthCheckResponsePayloadWrapper))
                //.filter(responsePayloadWrapper -> responsePayloadWrapper.getDeviceIdHashcode() == deviceIdHashCode)
                .map(HealthCheckResponsePayloadWrapper::getHealthCheckResult)
                .firstOrError();
    }

}
