package com.nexenio.seamlessauthenticationintegrationsample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AuthenticatorAdapter extends RecyclerView.Adapter<AuthenticatorViewHolder> {

    @NonNull
    private List<SeamlessAuthenticator> authenticators;

    @NonNull
    private final AuthenticatorListActivity parentActivity;

    @NonNull
    private final View.OnClickListener viewHolderClickListener;

    private final boolean useDetailFragment;

    public AuthenticatorAdapter(@NonNull AuthenticatorListActivity parentActivity, boolean useDetailFragment) {
        this.authenticators = new ArrayList<>();
        this.parentActivity = parentActivity;
        this.useDetailFragment = useDetailFragment;
        this.viewHolderClickListener = createViewHolderClickListener();
    }

    private View.OnClickListener createViewHolderClickListener() {
        return view -> {
            SeamlessAuthenticator authenticator = (SeamlessAuthenticator) view.getTag();
            if (useDetailFragment) {
                showInDetailFragment(authenticator);
            } else {
                showInDetailActivity(authenticator, view.getContext());
            }
        };
    }

    private void showInDetailFragment(@NonNull SeamlessAuthenticator authenticator) {
        Bundle arguments = new Bundle();
        arguments.putString(AuthenticatorDetailFragment.KEY_AUTHENTICATOR_ID, authenticator.getId().blockingGet().toString());
        AuthenticatorDetailFragment fragment = new AuthenticatorDetailFragment();
        fragment.setArguments(arguments);
        parentActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.authenticator_detail_container, fragment)
                .commit();
    }

    private void showInDetailActivity(@NonNull SeamlessAuthenticator authenticator, @NonNull Context context) {
        Intent intent = new Intent(context, AuthenticatorDetailActivity.class);
        intent.putExtra(AuthenticatorDetailFragment.KEY_AUTHENTICATOR_ID, authenticator.getId().blockingGet().toString());
        context.startActivity(intent);
    }

    @NonNull
    @Override
    public AuthenticatorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.authenticator_list_content, parent, false);
        return new AuthenticatorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuthenticatorViewHolder holder, int position) {
        SeamlessAuthenticator authenticator = authenticators.get(position);
        holder.renderAuthenticator(authenticator);
        holder.itemView.setTag(authenticator);
        holder.itemView.setOnClickListener(viewHolderClickListener);
    }

    @Override
    public int getItemCount() {
        return authenticators.size();
    }

    public void setAuthenticators(@NonNull List<SeamlessAuthenticator> authenticators) {
        this.authenticators = authenticators;
    }

}
