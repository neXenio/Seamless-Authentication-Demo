package com.nexenio.seamlessauthenticationintegrationsample.visualization;

import com.nexenio.seamlessauthentication.accesscontrol.gate.Gate;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.lock.DirectionLockBeacon;

import androidx.annotation.NonNull;

public interface GateVisualization extends CommunicationUnitVisualization {

    void onCommunicationUnitUpdated(@NonNull Gate gate);

    void onDetectionBeaconUpdated(@NonNull GatewayDetectionBeacon gatewayDetectionBeacon);

    void onDirectionLockBeaconUpdated(@NonNull DirectionLockBeacon gatewayDetectionBeacon);

}
