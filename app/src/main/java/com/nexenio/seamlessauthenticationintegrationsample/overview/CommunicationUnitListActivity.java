package com.nexenio.seamlessauthenticationintegrationsample.overview;

import android.os.Bundle;

import com.nexenio.seamlessauthentication.CommunicationUnit;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SeamlessAuthenticationActivity;
import com.nexenio.seamlessauthenticationintegrationsample.detail.CommunicationUnitDetailActivity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * An activity representing a list of {@link CommunicationUnit}s. This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity presents a list of
 * items, which when touched, lead to a {@link CommunicationUnitDetailActivity} representing item
 * details. On tablets, the activity presents the list of items and item details side-by-side using
 * two vertical panes.
 */
public class CommunicationUnitListActivity extends SeamlessAuthenticationActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean useDetailFragment;

    private Disposable communicationUnitListUpdateDisposable;

    private CommunicationUnitAdapter communicationUnitAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbarLayout.setTitle(getString(R.string.title_communication_unit_list));
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_communication_unit_list);
    }

    @Override
    protected void initializeViews() {
        super.initializeViews();
        if (findViewById(R.id.communication_unit_detail_container) != null) {
            useDetailFragment = true;
        }

        communicationUnitAdapter = new CommunicationUnitAdapter(this, useDetailFragment);
        RecyclerView recyclerView = findViewById(R.id.communication_unit_list);
        recyclerView.setAdapter(communicationUnitAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingCommunicationUnitList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingCommunicationUnitList();
    }

    private void startUpdatingCommunicationUnitList() {
        Timber.d("startUpdatingCommunicationUnitList() called");
        communicationUnitListUpdateDisposable = Flowable.interval(1, TimeUnit.SECONDS)
                .flatMapSingle(interval -> communicationUnitDetector.getCurrentlyDetectedCommunicationUnits().toList())
                .onErrorReturnItem(Collections.emptyList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showCommunicationUnits);
    }

    private void stopUpdatingCommunicationUnitList() {
        Timber.d("stopUpdatingCommunicationUnitList() called");
        if (communicationUnitListUpdateDisposable != null && !communicationUnitListUpdateDisposable.isDisposed()) {
            communicationUnitListUpdateDisposable.dispose();
        }
    }

    private void showCommunicationUnits(@NonNull List<CommunicationUnit> communicationUnits) {
        communicationUnitAdapter.setCommunicationUnits(communicationUnits);
        communicationUnitAdapter.notifyDataSetChanged();
    }

}
