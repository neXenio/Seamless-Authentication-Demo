package com.nexenio.seamlessauthenticationintegrationsample.overview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nexenio.seamlessauthentication.CommunicationUnit;
import com.nexenio.seamlessauthenticationintegrationsample.R;
import com.nexenio.seamlessauthenticationintegrationsample.SeamlessAuthenticationActivity;
import com.nexenio.seamlessauthenticationintegrationsample.detail.CommunicationUnitDetailActivity;
import com.nexenio.seamlessauthenticationintegrationsample.detail.CommunicationUnitDetailFragment;
import com.nexenio.seamlessauthenticationintegrationsample.health.CommunicationUnitHealthActivity;
import com.nexenio.seamlessauthenticationintegrationsample.health.CommunicationUnitHealthFragment;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class CommunicationUnitAdapter extends RecyclerView.Adapter<CommunicationUnitViewHolder> {

    @NonNull
    private List<CommunicationUnit> communicationUnits;

    @NonNull
    private final CommunicationUnitListActivity parentActivity;

    @NonNull
    private final View.OnClickListener viewHolderClickListener;

    private final boolean useDetailFragment;

    public CommunicationUnitAdapter(@NonNull CommunicationUnitListActivity parentActivity, boolean useDetailFragment) {
        this.communicationUnits = new ArrayList<>();
        this.parentActivity = parentActivity;
        this.useDetailFragment = useDetailFragment;
        this.viewHolderClickListener = createViewHolderClickListener();
    }

    private View.OnClickListener createViewHolderClickListener() {
        return view -> {
            CommunicationUnit communicationUnit = (CommunicationUnit) view.getTag();
            if (useDetailFragment) {
                showInDetailFragment(communicationUnit);
            } else {
                showInDetailActivity(view.getContext(), communicationUnit);
            }
        };
    }

    private void showInFragment(@NonNull CommunicationUnit communicationUnit, @NonNull Fragment fragment) {
        Bundle arguments = new Bundle();
        arguments.putString(CommunicationUnitDetailFragment.KEY_COMMUNICATION_UNIT_ID, communicationUnit.getId().blockingGet().toString());
        fragment.setArguments(arguments);
        parentActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.communication_unit_detail_container, fragment)
                .commit();
    }

    private void showInActivity(@NonNull Context context, @NonNull CommunicationUnit communicationUnit,
                                @NonNull Class<? extends SeamlessAuthenticationActivity> activity) {
        Intent intent = new Intent(context, activity);
        intent.putExtra(CommunicationUnitDetailFragment.KEY_COMMUNICATION_UNIT_ID, communicationUnit.getId().blockingGet().toString());
        context.startActivity(intent);
    }

    private void showInDetailFragment(@NonNull CommunicationUnit communicationUnit) {
        showInFragment(communicationUnit, new CommunicationUnitDetailFragment());
    }

    private void showInDetailActivity(@NonNull Context context, @NonNull CommunicationUnit communicationUnit) {
        showInActivity(context, communicationUnit, CommunicationUnitDetailActivity.class);
    }

    private void showInHealthFragment(@NonNull CommunicationUnit communicationUnit) {
        showInFragment(communicationUnit, new CommunicationUnitHealthFragment());
    }

    private void showInHealthActivity(@NonNull Context context, @NonNull CommunicationUnit communicationUnit) {
        showInActivity(context, communicationUnit, CommunicationUnitHealthActivity.class);
    }

    @NonNull
    @Override
    public CommunicationUnitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.communication_unit_list_content, parent, false);
        return new CommunicationUnitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommunicationUnitViewHolder holder, int position) {
        CommunicationUnit communicationUnit = communicationUnits.get(position);
        holder.renderCommunicationUnit(communicationUnit);
        holder.itemView.setTag(communicationUnit);
        holder.itemView.setOnClickListener(viewHolderClickListener);
    }

    @Override
    public int getItemCount() {
        return communicationUnits.size();
    }

    public void setCommunicationUnits(@NonNull List<CommunicationUnit> communicationUnits) {
        this.communicationUnits = communicationUnits;
    }

}
