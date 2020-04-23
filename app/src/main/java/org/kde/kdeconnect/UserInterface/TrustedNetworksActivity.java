package org.kde.kdeconnect.UserInterface;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.kde.kdeconnect.Helpers.TrustedNetworkHelper;
import org.kde.kdeconnect.R;

import java.util.ArrayList;
import java.util.List;

public class TrustedNetworksActivity extends AppCompatActivity {

    private List<String> trustedNetworks;

    private ListView trustedNetworksView;
    private CheckBox allowAllCheckBox;
    private TrustedNetworkHelper trustedNetworkHelper;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean grantedPermission = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                grantedPermission = true;
                break;
            }
        }
        if (grantedPermission) {
            allowAllCheckBox.setChecked(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeUtil.setUserPreferredTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trusted_network_list);
        trustedNetworksView = findViewById(android.R.id.list);

        trustedNetworkHelper = new TrustedNetworkHelper(getApplicationContext());
        trustedNetworks = new ArrayList<>(trustedNetworkHelper.read());

        allowAllCheckBox = findViewById(R.id.trust_all_networks_checkBox);
        allowAllCheckBox.setOnCheckedChangeListener((v, isChecked) -> {

            if (trustedNetworkHelper.hasPermissions()) {
                trustedNetworkHelper.allAllowed(isChecked);
                updateTrustedNetworkListView();
                addNetworkButton();
            } else {
                allowAllCheckBox.setChecked(true); // Disable unchecking it
                new PermissionsAlertDialogFragment.Builder()
                        .setTitle(R.string.location_permission_needed_title)
                        .setMessage(R.string.location_permission_needed_desc)
                        .setPositiveButton(R.string.ok)
                        .setNegativeButton(R.string.cancel)
                        .setPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION})
                        .setRequestCode(0)
                        .create().show(getSupportFragmentManager(), null);
            }
        });
        allowAllCheckBox.setChecked(trustedNetworkHelper.allAllowed());

        updateTrustedNetworkListView();
    }

    private void updateEmptyListMessage() {
        boolean isVisible = trustedNetworks.isEmpty() && !trustedNetworkHelper.allAllowed();
        findViewById(R.id.trusted_network_list_empty)
                .setVisibility(isVisible ? View.VISIBLE : View.GONE );
    }

    private void updateTrustedNetworkListView() {
        Boolean allAllowed = trustedNetworkHelper.allAllowed();
        updateEmptyListMessage();
        trustedNetworksView.setVisibility(allAllowed ? View.GONE : View.VISIBLE);
        if (allAllowed){
            return;
        }
        trustedNetworksView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, trustedNetworks));
        trustedNetworksView.setOnItemClickListener((parent, view, position, id) -> {
            String targetItem = trustedNetworks.get(position);
            new AlertDialog.Builder(TrustedNetworksActivity.this)
                .setMessage("Delete " + targetItem + " ?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    trustedNetworks.remove(position);
                    trustedNetworkHelper.update(trustedNetworks);
                    ((ArrayAdapter) trustedNetworksView.getAdapter()).notifyDataSetChanged();
                    addNetworkButton();
                    updateEmptyListMessage();
                })
                .setNegativeButton("No", null)
                .show();

        });
        addNetworkButton();
    }


    private void addNetworkButton() {
        Button addButton = findViewById(android.R.id.button1);
        if (trustedNetworkHelper.allAllowed()) {
            addButton.setVisibility(View.GONE);
            return;
        }
        final String currentSSID = trustedNetworkHelper.currentSSID();
        if (!currentSSID.isEmpty() && trustedNetworks.indexOf(currentSSID) == -1) {
            String buttonText = getString(R.string.add_trusted_network, currentSSID);
            addButton.setText(buttonText);
            addButton.setOnClickListener(v -> {
                if (trustedNetworks.indexOf(currentSSID) != -1){
                    return;
                }
                trustedNetworks.add(currentSSID);
                trustedNetworkHelper.update(trustedNetworks);
                ((ArrayAdapter) trustedNetworksView.getAdapter()).notifyDataSetChanged();
                v.setVisibility(View.GONE);
                updateEmptyListMessage();
            });
            addButton.setVisibility(View.VISIBLE);
        } else {
            addButton.setVisibility(View.GONE);
        }
    }

}
