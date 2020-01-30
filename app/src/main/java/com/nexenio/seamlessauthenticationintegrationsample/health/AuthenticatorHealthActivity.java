package com.nexenio.seamlessauthenticationintegrationsample.health;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SeamlessAuthenticationActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.AuthenticatorListActivity;

import androidx.appcompat.app.ActionBar;

/**
 * An activity representing a single Authenticator health check screen. This activity is only used
 * on narrow width devices. On tablet-size devices, item details are presented side-by-side with a
 * list of items in a {@link AuthenticatorListActivity}.
 */
public class AuthenticatorHealthActivity extends SeamlessAuthenticationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbarLayout.setTitle(getString(R.string.authenticator_name_unknown));

        if (savedInstanceState == null) {
            String idExtra = getIntent().getStringExtra(AuthenticatorHealthFragment.KEY_AUTHENTICATOR_ID);

            Bundle arguments = new Bundle();
            arguments.putString(AuthenticatorHealthFragment.KEY_AUTHENTICATOR_ID, idExtra);

            // toolbarLayout.setTitle(title);

            AuthenticatorHealthFragment fragment = new AuthenticatorHealthFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.authenticator_health_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_authenticator_health);
    }

    @Override
    protected void initializeViews() {
        super.initializeViews();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        stopSeamlessAuthenticatorDetection();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, AuthenticatorListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}
