package com.nexenio.seamlessauthenticationintegrationsample.health.sblec;

import com.nexenio.sblec.payload.PayloadWrapper;
import com.nexenio.seamlessauthentication.SeamlessAuthenticationException;
import com.nexenio.seamlessauthentication.SeamlessAuthenticator;

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

    private UUID authenticatorId;

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

            buffer.putLong(authenticatorId.getMostSignificantBits());
            buffer.putLong(authenticatorId.getLeastSignificantBits());
            buffer.put((byte) nonce);

            return buffer;
        });
    }

    @Override
    public int getId() {
        return ID;
    }

    public UUID getAuthenticatorId() {
        return authenticatorId;
    }

    public void setAuthenticatorId(UUID authenticatorId) {
        this.authenticatorId = authenticatorId;
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

        public Builder setAuthenticator(@NonNull SeamlessAuthenticator authenticator) {
            return setAuthenticatorId(authenticator.getId().blockingGet());
        }

        public Builder setAuthenticatorId(@NonNull UUID authenticatorId) {
            payloadWrapper.authenticatorId = authenticatorId;
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
