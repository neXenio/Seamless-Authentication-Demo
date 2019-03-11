package com.nexenio.seamlessauthenticationintegrationsample;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;

/**
 * An activity representing a single Authenticator detail screen. This activity is only used on
 * narrow width devices. On tablet-size devices, item details are presented side-by-side with a list
 * of items in a {@link AuthenticatorListActivity}.
 */
public class AuthenticatorDetailActivity extends SeamlessAuthenticationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbarLayout.setTitle(getString(R.string.authenticator_name_unknown));

        if (savedInstanceState == null) {
            String idExtra = getIntent().getStringExtra(AuthenticatorDetailFragment.KEY_AUTHENTICATOR_ID);

            Bundle arguments = new Bundle();
            arguments.putString(AuthenticatorDetailFragment.KEY_AUTHENTICATOR_ID, idExtra);

            // toolbarLayout.setTitle(title);

            AuthenticatorDetailFragment fragment = new AuthenticatorDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.authenticator_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_authenticator_detail);
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
