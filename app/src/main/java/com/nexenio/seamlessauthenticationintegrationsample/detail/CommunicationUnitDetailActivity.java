package com.nexenio.seamlessauthenticationintegrationsample.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.nexenio.seamlessauthentication.CommunicationUnit;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SeamlessAuthenticationActivity;
import com.nexenio.seamlessauthenticationintegrationsample.overview.CommunicationUnitListActivity;

import androidx.appcompat.app.ActionBar;

/**
 * An activity representing a single {@link CommunicationUnit} detail screen. This activity is only
 * used on narrow width devices. On tablet-size devices, item details are presented side-by-side
 * with a list of items in a {@link CommunicationUnitListActivity}.
 */
public class CommunicationUnitDetailActivity extends SeamlessAuthenticationActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbarLayout.setTitle(getString(R.string.communication_unit_name_unknown));

        if (savedInstanceState == null) {
            String idExtra = getIntent().getStringExtra(CommunicationUnitDetailFragment.KEY_COMMUNICATION_UNIT_ID);

            Bundle arguments = new Bundle();
            arguments.putString(CommunicationUnitDetailFragment.KEY_COMMUNICATION_UNIT_ID, idExtra);

            // toolbarLayout.setTitle(title);

            CommunicationUnitDetailFragment fragment = new CommunicationUnitDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.communication_unit_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_communication_unit_detail);
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
            navigateUpTo(new Intent(this, CommunicationUnitListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
