/*
 * Copyright 2015 Aleix Pol Gonzalez <aleixpol@kde.org>
 * Copyright 2015 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License or (at your option) version 3 or any later version
 * accepted by the membership of KDE e.V. (or its successor approved
 * by the membership of KDE e.V.), which shall act as a proxy
 * defined in Section 14 of version 3 of the license.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kde.kdeconnect.Plugins.RunCommandPlugin;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.R;
import org.kde.kdeconnect.UserInterface.List.ListAdapter;
import org.kde.kdeconnect.UserInterface.ThemeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class RunCommandActivity extends AppCompatActivity {

    private String deviceId;
    private final RunCommandPlugin.CommandsChangedCallback commandsChangedCallback = new RunCommandPlugin.CommandsChangedCallback() {
        @Override
        public void update() {
            RunCommandActivity.this.updateView();
        }
    };
    private ArrayList<ListAdapter.Item> commandItems;

    private void updateView() {

        BackgroundService.RunWithPlugin(this, deviceId, RunCommandPlugin.class, new BackgroundService.PluginCallback<RunCommandPlugin>() {
            @Override
            public void run(final RunCommandPlugin plugin) {
                RunCommandActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ListView view = RunCommandActivity.this.findViewById(R.id.runcommandslist);

                        RunCommandActivity.this.registerForContextMenu(view);

                        commandItems = new ArrayList<>();
                        for (JSONObject obj : plugin.getCommandList()) {
                            try {
                                commandItems.add(new CommandEntry(obj.getString("name"),
                                        obj.getString("command"), obj.getString("key")));
                            } catch (JSONException e) {
                                Log.e("RunCommand", "Error parsing JSON", e);
                            }
                        }

                        Collections.sort(commandItems, new Comparator<ListAdapter.Item>() {
                            @Override
                            public int compare(ListAdapter.Item lhs, ListAdapter.Item rhs) {
                                String lName = ((CommandEntry) lhs).getName();
                                String rName = ((CommandEntry) rhs).getName();
                                return lName.compareTo(rName);
                            }
                        });

                        ListAdapter adapter = new ListAdapter(RunCommandActivity.this, commandItems);

                        view.setAdapter(adapter);
                        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view1, int i, long l) {
                                CommandEntry entry = (CommandEntry) commandItems.get(i);
                                plugin.runCommand(entry.getKey());
                            }
                        });


                        TextView explanation = RunCommandActivity.this.findViewById(R.id.addcomand_explanation);
                        String text = RunCommandActivity.this.getString(R.string.addcommand_explanation);
                        if (!plugin.canAddCommand()) {
                            text += "\n" + RunCommandActivity.this.getString(R.string.addcommand_explanation2);
                        }
                        explanation.setText(text);
                        explanation.setVisibility(commandItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setUserPreferredTheme(this);
        setContentView(R.layout.activity_runcommand);

        deviceId = getIntent().getStringExtra("deviceId");

        boolean canAddCommands = false;
        try {
            canAddCommands = BackgroundService.getInstance().getDevice(deviceId).getPlugin(RunCommandPlugin.class).canAddCommand();
        } catch (Exception ignore) {
        }

        FloatingActionButton addCommandButton = findViewById(R.id.add_command_button);
        if (canAddCommands) {
            addCommandButton.show();
        } else {
            addCommandButton.hide();
        }

        addCommandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BackgroundService.RunWithPlugin(RunCommandActivity.this, deviceId, RunCommandPlugin.class, new BackgroundService.PluginCallback<RunCommandPlugin>() {
                    @Override
                    public void run(RunCommandPlugin plugin) {
                        plugin.sendSetupPacket();
                        new AlertDialog.Builder(RunCommandActivity.this)
                                .setTitle(R.string.add_command)
                                .setMessage(R.string.add_command_description)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                });
            }
        });
        updateView();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.runcommand_context, menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.copy_url_to_clipboard) {
            CommandEntry entry = (CommandEntry) commandItems.get(info.position);
            String url = "kdeconnect://runcommand/" + deviceId + "/" + entry.getKey();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setText(url);
            Toast toast = Toast.makeText(this, R.string.clipboard_toast, Toast.LENGTH_SHORT);
            toast.show();
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        BackgroundService.RunWithPlugin(this, deviceId, RunCommandPlugin.class, new BackgroundService.PluginCallback<RunCommandPlugin>() {
            @Override
            public void run(RunCommandPlugin plugin) {
                plugin.addCommandsUpdatedCallback(commandsChangedCallback);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        BackgroundService.RunWithPlugin(this, deviceId, RunCommandPlugin.class, new BackgroundService.PluginCallback<RunCommandPlugin>() {
            @Override
            public void run(RunCommandPlugin plugin) {
                plugin.removeCommandsUpdatedCallback(commandsChangedCallback);
            }
        });
    }
}
