package com.nexenio.seamlessauthenticationintegrationsample.health.sblec;

import me.seamless.sblec.payload.PayloadWrapper;
import me.seamless.sblec.receiver.ReceiverPayload;
import com.nexenio.seamlessauthentication.SeamlessAuthenticationException;
import com.nexenio.seamlessauthenticationintegrationsample.health.HealthCheckResult;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * See: <a href="https://confluence.nexenio.com/display/BA/Health+Check+Response">Documentation</a>
 */
public class HealthCheckResponsePayloadWrapper extends PayloadWrapper {

    public static final short ID = 120;

    private int deviceIdHashcode;

    private HealthCheckResult healthCheckResult;

    private int nonce;

    public HealthCheckResponsePayloadWrapper(@NonNull ReceiverPayload receiverPayload) {
        super(receiverPayload);

    }

    @Override
    public Completable readFromBuffer(@NonNull ByteBuffer byteBuffer) {
        return Completable.fromAction(() -> {
            byteBuffer.rewind();

            deviceIdHashcode = byteBuffer.getShort();

            healthCheckResult = new HealthCheckResult();
            healthCheckResult.setOperationalBluetoothChips(byteBuffer.get());
            healthCheckResult.setActiveDevices(byteBuffer.get());

            nonce = byteBuffer.get();
        });
    }

    @Override
    public Single<ByteBuffer> writeToBuffer() {
        return Single.error(new SeamlessAuthenticationException("Not supported"));
    }

    @Override
    public String toString() {
        return "HealthCheckResponsePayloadWrapper{" +
                "deviceIdHashcode=" + deviceIdHashcode +
                ", healthCheckResult=" + healthCheckResult +
                ", nonce=" + nonce +
                '}';
    }

    @Override
    public int getId() {
        return ID;
    }

    public int getDeviceIdHashcode() {
        return deviceIdHashcode;
    }

    public HealthCheckResult getHealthCheckResult() {
        return healthCheckResult;
    }

    public int getNonce() {
        return nonce;
    }

}
