package com.nexenio.seamlessauthenticationintegrationsample.health.sblec;

import android.content.Context;

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
import me.seamless.sblec.PerformanceModes;
import me.seamless.sblec.Sblec;
import me.seamless.sblec.payload.PayloadIdFilter;
import me.seamless.sblec.receiver.CompletelyReceivedFilter;
import me.seamless.sblec.receiver.PayloadReceiver;
import me.seamless.sblec.sender.PayloadSender;
import me.seamless.sblec.sender.SenderPayload;
import timber.log.Timber;

public class SblecHealthManager {

    private final UUID communicationUnitId;

    private Sblec sblec;
    private int deviceIdHashCode;
    private PayloadSender sender;
    private PayloadReceiver receiver;
    private int nonce;

    public SblecHealthManager(UUID communicationUnitId) {
        this.communicationUnitId = communicationUnitId;
    }

    public Completable initialize(@NonNull Context context) {
        return Completable.fromAction(() -> {
            sblec = Sblec.getInstance();
            deviceIdHashCode = (short) sblec.getOrCreateDeviceIdHashCode(context);
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
        return Single.defer(() -> {
            HealthCheckRequestPayloadWrapper payloadWrapper = new HealthCheckRequestPayloadWrapper.Builder()
                    .setCommunicationUnitId(communicationUnitId)
                    .build();

            this.nonce = payloadWrapper.getNonce();
            Timber.d("Current health check request nonce: %d", nonce);

            return payloadWrapper.toSenderPayload();
        });
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
        return receiver.receive()
                .filter(new PayloadIdFilter(HealthCheckResponsePayloadWrapper.ID))
                .filter(new CompletelyReceivedFilter())
                .doOnNext(receiverPayload -> Timber.v("Received health check response: %s", receiverPayload))
                .map(HealthCheckResponsePayloadWrapper::new)
                .doOnNext(payloadWrapper -> {
                    if (payloadWrapper.getNonce() == nonce) {
                        Timber.i("Received health check response: %s", payloadWrapper);
                    } else {
                        Timber.w("Received health check response, but nonce doesn't match: %s", payloadWrapper);
                    }
                })
                .filter(responsePayloadWrapper -> responsePayloadWrapper.getDeviceIdHashcode() == deviceIdHashCode)
                .filter(healthCheckResponsePayloadWrapper -> healthCheckResponsePayloadWrapper.getNonce() == nonce)
                .map(HealthCheckResponsePayloadWrapper::getHealthCheckResult)
                .firstOrError();
    }

}
