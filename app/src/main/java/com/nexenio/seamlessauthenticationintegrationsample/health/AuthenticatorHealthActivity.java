package com.nexenio.seamlessauthenticationintegrationsample.health;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, AuthenticatorListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
