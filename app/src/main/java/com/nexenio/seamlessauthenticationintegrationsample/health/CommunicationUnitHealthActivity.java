package com.nexenio.seamlessauthenticationintegrationsample.health;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.nexenio.seamlessauthentication.CommunicationUnit;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SeamlessAuthenticationActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.CommunicationUnitListActivity;

import androidx.appcompat.app.ActionBar;

/**
 * An activity representing a single {@link CommunicationUnit} health check screen. This activity is
 * only used on narrow width devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link CommunicationUnitListActivity}.
 */
public class CommunicationUnitHealthActivity extends SeamlessAuthenticationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbarLayout.setTitle(getString(R.string.communication_unit_name_unknown));

        if (savedInstanceState == null) {
            String idExtra = getIntent().getStringExtra(CommunicationUnitHealthFragment.KEY_COMMUNICATION_UNIT_ID);

            Bundle arguments = new Bundle();
            arguments.putString(CommunicationUnitHealthFragment.KEY_COMMUNICATION_UNIT_ID, idExtra);

            // toolbarLayout.setTitle(title);

            CommunicationUnitHealthFragment fragment = new CommunicationUnitHealthFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.communication_unit_health_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_communication_unit_health);
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
        stopCommunicationUnitDetection();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, CommunicationUnitListActivity.class));
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
