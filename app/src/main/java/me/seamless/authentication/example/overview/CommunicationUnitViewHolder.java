package me.seamless.authentication.example.overview;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import me.seamless.authentication.CommunicationUnit;
import me.seamless.authentication.accesscontrol.gate.Gate;
import me.seamless.authentication.accesscontrol.gateway.Gateway;
import me.seamless.authentication.accesscontrol.gateway.GatewayDirection;
import me.seamless.authentication.accesscontrol.gateway.opening.GatewayOpening;
import me.seamless.authentication.example.R;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;

public class CommunicationUnitViewHolder extends RecyclerView.ViewHolder {

    private final TextView titleTextView;
    private final TextView subtitleTextView;
    private final TextView contentTextView;

    CommunicationUnitViewHolder(View view) {
        super(view);
        titleTextView = view.findViewById(R.id.titleTextView);
        subtitleTextView = view.findViewById(R.id.subtitleTextView);
        contentTextView = view.findViewById(R.id.contentTextView);
    }

    void renderCommunicationUnit(@NonNull CommunicationUnit communicationUnit) {
        Context context = itemView.getContext();

        String title = getReadableName(communicationUnit, context).blockingGet();
        String subtitle = getReadableId(communicationUnit, context).blockingGet();
        String content = getReadableDescription(communicationUnit, context).blockingGet();

        titleTextView.setText(title);
        subtitleTextView.setText(subtitle);
        contentTextView.setText(content);
    }

    public static Single<String> getReadableDescription(@NonNull CommunicationUnit communicationUnit, @NonNull Context context) {
        return Single.fromCallable(() -> {
            double distance = communicationUnit.getDistance().blockingGet();
            String state = getReadableState(communicationUnit, context).blockingGet();

            if (communicationUnit instanceof Gate) {
                Gate gate = (Gate) communicationUnit;

                Gateway closestGateway = gate.getClosestGateway().blockingGet();
                String direction = getReadableDirection(closestGateway, context).blockingGet();

                long gatewaysCount = gate.getGateways().count().blockingGet();
                if (gatewaysCount > 1) {
                    return context.getString(R.string.communication_unit_gate_description,
                            distance, state, gatewaysCount, direction, closestGateway.getGatewayIndex().blockingGet());
                } else {
                    return context.getString(R.string.communication_unit_door_description,
                            distance, state, direction);
                }
            } else {
                return context.getString(R.string.communication_unit_generic_description,
                        distance, state);
            }
        }).onErrorReturnItem(context.getString(R.string.communication_unit_name_unknown));
    }

    public static Single<String> getReadableName(@NonNull CommunicationUnit communicationUnit, @NonNull Context context) {
        return communicationUnit.getName()
                .onErrorReturnItem(context.getString(R.string.communication_unit_name_unknown));
    }

    public static Single<String> getReadableId(@NonNull CommunicationUnit communicationUnit, @NonNull Context context) {
        return communicationUnit.getId().map(UUID::toString)
                .onErrorReturnItem(context.getString(R.string.communication_unit_id_unknown));
    }

    public static Single<String> getReadableState(@NonNull CommunicationUnit CommunicationUnit, @NonNull Context context) {
        return CommunicationUnit.isActive().map(active -> {
            int resourceId = active ? R.string.communication_unit_state_active : R.string.communication_unit_state_inactive;
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
