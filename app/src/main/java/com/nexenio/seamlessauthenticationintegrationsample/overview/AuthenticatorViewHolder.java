package com.nexenio.seamlessauthenticationintegrationsample.overview;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.accesscontrol.gate.Gate;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.Gateway;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.GatewayDirection;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.opening.GatewayOpening;
import com.nexenio.seamlessauthenticationintegrationsample.R;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;

public class AuthenticatorViewHolder extends RecyclerView.ViewHolder {

    private final TextView titleTextView;
    private final TextView subtitleTextView;
    private final TextView contentTextView;

    AuthenticatorViewHolder(View view) {
        super(view);
        titleTextView = view.findViewById(R.id.titleTextView);
        subtitleTextView = view.findViewById(R.id.subtitleTextView);
        contentTextView = view.findViewById(R.id.contentTextView);
    }

    void renderAuthenticator(@NonNull SeamlessAuthenticator authenticator) {
        Context context = itemView.getContext();

        String title = getReadableName(authenticator, context).blockingGet();
        String subtitle = getReadableId(authenticator, context).blockingGet();
        String content = getReadableDescription(authenticator, context).blockingGet();

        titleTextView.setText(title);
        subtitleTextView.setText(subtitle);
        contentTextView.setText(content);
    }

    public static Single<String> getReadableDescription(@NonNull SeamlessAuthenticator authenticator, @NonNull Context context) {
        return Single.fromCallable(() -> {
            double distance = authenticator.getDistance().blockingGet();
            String state = getReadableState(authenticator, context).blockingGet();

            if (authenticator instanceof Gate) {
                Gate gate = (Gate) authenticator;

                Gateway closestGateway = gate.getClosestGateway().blockingGet();
                String direction = getReadableDirection(closestGateway, context).blockingGet();

                long gatewaysCount = gate.getGateways().count().blockingGet();
                if (gatewaysCount > 1) {
                    return context.getString(R.string.authenticator_gate_description,
                            distance, state, gatewaysCount, direction, closestGateway.getIndex().blockingGet());
                } else {
                    return context.getString(R.string.authenticator_door_description,
                            distance, state, direction);
                }
            } else {
                return context.getString(R.string.authenticator_generic_description,
                        distance, state);
            }
        }).onErrorReturnItem(context.getString(R.string.authenticator_name_unknown));
    }

    public static Single<String> getReadableName(@NonNull SeamlessAuthenticator authenticator, @NonNull Context context) {
        return authenticator.getName()
                .onErrorReturnItem(context.getString(R.string.authenticator_name_unknown));
    }

    public static Single<String> getReadableId(@NonNull SeamlessAuthenticator authenticator, @NonNull Context context) {
        return authenticator.getId().map(UUID::toString)
                .onErrorReturnItem(context.getString(R.string.authenticator_id_unknown));
    }

    public static Single<String> getReadableState(@NonNull SeamlessAuthenticator authenticator, @NonNull Context context) {
        return authenticator.isActive().map(active -> {
            int resourceId = active ? R.string.authenticator_state_active : R.string.authenticator_state_inactive;
            return context.getString(resourceId);
        }).onErrorReturnItem(context.getString(R.string.unknown));
    }

    public static Single<String> getReadableDirection(@NonNull Gateway gateway, @NonNull Context context) {
        return gateway.getClosestOpening()
                .flatMap(GatewayOpening::getDirection)
                .map(direction -> {
                    int resourceId = direction == GatewayDirection.ENTRY ?
                            R.string.gateway_direction_entry : R.string.gateway_direction_exit;
                    return context.getString(resourceId);
                }).onErrorReturnItem(context.getString(R.string.unknown));
    }

}
