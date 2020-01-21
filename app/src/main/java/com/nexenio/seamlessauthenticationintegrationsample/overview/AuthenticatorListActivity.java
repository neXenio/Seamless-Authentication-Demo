package com.nexenio.seamlessauthenticationintegrationsample.overview;

import android.os.Bundle;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SeamlessAuthenticationActivity;
import com.nexenio.seamlessauthenticationintegrationsample.detail.AuthenticatorDetailActivity;

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
 * An activity representing a list of Authenticators. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link AuthenticatorDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two vertical panes.
 */
public class AuthenticatorListActivity extends SeamlessAuthenticationActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean useDetailFragment;

    private Disposable authenticatorListUpdateDisposable;

    private AuthenticatorAdapter authenticatorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        toolbarLayout.setTitle(getString(R.string.title_authenticator_list));
    }

    @Override
    protected void setContentView() {
        setContentView(R.layout.activity_authenticator_list);
    }

    @Override
    protected void initializeViews() {
        super.initializeViews();
        if (findViewById(R.id.authenticator_detail_container) != null) {
            useDetailFragment = true;
        }

        authenticatorAdapter = new AuthenticatorAdapter(this, useDetailFragment);
        RecyclerView recyclerView = findViewById(R.id.authenticator_list);
        recyclerView.setAdapter(authenticatorAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingAuthenticatorList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingAuthenticatorList();
    }

    private void startUpdatingAuthenticatorList() {
        Timber.d("startUpdatingAuthenticatorList() called");
        authenticatorListUpdateDisposable = Flowable.interval(1, TimeUnit.SECONDS)
                .flatMapSingle(authenticator -> authenticatorDetector.getDetectedAuthenticators().toList())
                .onErrorReturnItem(Collections.emptyList())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showAuthenticators);
    }

    private void stopUpdatingAuthenticatorList() {
        Timber.d("stopUpdatingAuthenticatorList() called");
        if (authenticatorListUpdateDisposable != null && !authenticatorListUpdateDisposable.isDisposed()) {
            authenticatorListUpdateDisposable.dispose();
        }
    }

    private void showAuthenticators(@NonNull List<SeamlessAuthenticator> authenticators) {
        authenticatorAdapter.setAuthenticators(authenticators);
        authenticatorAdapter.notifyDataSetChanged();
    }

}
