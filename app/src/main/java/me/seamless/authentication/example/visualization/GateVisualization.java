package me.seamless.authentication.example.visualization;

import androidx.annotation.NonNull;
import me.seamless.authentication.accesscontrol.gate.Gate;
import me.seamless.authentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import me.seamless.authentication.internal.accesscontrol.beacons.lock.DirectionLockBeacon;

public interface GateVisualization extends CommunicationUnitVisualization {

    void onCommunicationUnitUpdated(@NonNull Gate gate);

    void onDetectionBeaconUpdated(@NonNull GatewayDetectionBeacon gatewayDetectionBeacon);

    void onDirectionLockBeaconUpdated(@NonNull DirectionLockBeacon gatewayDetectionBeacon);

}
