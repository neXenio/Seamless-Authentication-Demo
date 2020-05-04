package com.nexenio.seamlessauthenticationintegrationsample.health.sblec;

import me.seamless.sblec.payload.PayloadWrapper;
import com.nexenio.seamlessauthentication.CommunicationUnit;
import com.nexenio.seamlessauthentication.SeamlessAuthenticationException;

import java.nio.ByteBuffer;
import java.util.UUID;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * See: <a href="https://confluence.nexenio.com/display/BA/Health+Check+Request">Documentation</a>
 */
public class HealthCheckRequestPayloadWrapper extends PayloadWrapper {

    private static final short ID = 110;

    private UUID communicationUnitId;

    private int nonce;

    private HealthCheckRequestPayloadWrapper() {
    }

    @Override
    public Completable readFromBuffer(@NonNull ByteBuffer byteBuffer) {
        return Completable.error(new SeamlessAuthenticationException("Not supported"));
    }

    @Override
    public Single<ByteBuffer> writeToBuffer() {
        return Single.fromCallable(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(17);

            buffer.putLong(communicationUnitId.getMostSignificantBits());
            buffer.putLong(communicationUnitId.getLeastSignificantBits());
            buffer.put((byte) nonce);

            return buffer;
        });
    }

    @Override
    public int getId() {
        return ID;
    }

    public UUID getCommunicationUnitId() {
        return communicationUnitId;
    }

    public void setCommunicationUnitId(UUID communicationUnitId) {
        this.communicationUnitId = communicationUnitId;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public static class Builder {

        private HealthCheckRequestPayloadWrapper payloadWrapper;

        public Builder() {
            payloadWrapper = new HealthCheckRequestPayloadWrapper();
            payloadWrapper.setNonce((int) Math.round(Math.random() * 127));
        }

        public Builder setCommunicationUnit(@NonNull CommunicationUnit communicationUnit) {
            return setCommunicationUnitId(communicationUnit.getId().blockingGet());
        }

        public Builder setCommunicationUnitId(@NonNull UUID communicationUnitId) {
            payloadWrapper.communicationUnitId = communicationUnitId;
            return this;
        }

        public Builder setNonce(int nonce) {
            payloadWrapper.nonce = nonce;
            return this;
        }

        public HealthCheckRequestPayloadWrapper build() {
            return payloadWrapper;
        }

    }

}
