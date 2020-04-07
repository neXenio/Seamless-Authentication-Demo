package com.nexenio.seamlessauthenticationintegrationsample.visualization;

import com.nexenio.seamlessauthentication.CommunicationUnit;

import androidx.annotation.NonNull;

public interface CommunicationUnitVisualization {

    void onCommunicationUnitUpdated(@NonNull CommunicationUnit communicationUnit);

}
