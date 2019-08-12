package com.nexenio.seamlessauthenticationintegrationsample.visualization;

import com.nexenio.seamlessauthentication.accesscontrol.gate.Gate;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.lock.GatewayDirectionLockBeacon;

import androidx.annotation.NonNull;

public interface GateVisualization extends AuthenticatorVisualization {

    void onAuthenticatorUpdated(@NonNull Gate gate);

    void onDetectionBeaconUpdated(@NonNull GatewayDetectionBeacon gatewayDetectionBeacon);

    void onDirectionLockBeaconUpdated(@NonNull GatewayDirectionLockBeacon gatewayDetectionBeacon);

}
