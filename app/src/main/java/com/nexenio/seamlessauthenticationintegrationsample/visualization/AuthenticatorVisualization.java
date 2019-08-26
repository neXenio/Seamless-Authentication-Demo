package com.nexenio.seamlessauthenticationintegrationsample.visualization;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;

import androidx.annotation.NonNull;

public interface AuthenticatorVisualization {

    void onAuthenticatorUpdated(@NonNull SeamlessAuthenticator authenticator);

}
