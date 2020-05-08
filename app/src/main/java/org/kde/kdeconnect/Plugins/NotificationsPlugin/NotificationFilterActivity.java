/*
 * Copyright 2015 Vineet Garg <grg.vineet@gmail.com>
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

package org.kde.kdeconnect.Plugins.NotificationsPlugin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ListView;

import org.kde.kdeconnect.BackgroundService;
import org.kde.kdeconnect.R;
import org.kde.kdeconnect.UserInterface.ThemeUtil;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

//TODO: Turn this into a PluginSettingsFragment
public class NotificationFilterActivity extends AppCompatActivity {

    private AppDatabase appDatabase;
    private ListView listView;

    static class AppListInfo {

        String pkg;
        String name;
        Drawable icon;
        boolean isEnabled;
    }

    private AppListInfo[] apps;

    class AppListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return apps.length + 1;
        }

        @Override
        public AppListInfo getItem(int position) {
            return apps[position - 1];
        }

        @Override
        public long getItemId(int position) {
            return position - 1;
        }

        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                LayoutInflater inflater = getLayoutInflater();
                view = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, null, true);
            }
            CheckedTextView checkedTextView = (CheckedTextView) view;
            if (position == 0) {
                checkedTextView.setText(R.string.all);
                checkedTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            } else {
                checkedTextView.setText(apps[position - 1].name);
                checkedTextView.setCompoundDrawablesWithIntrinsicBounds(apps[position - 1].icon, null, null, null);
                checkedTextView.setCompoundDrawablePadding((int) (8 * getResources().getDisplayMetrics().density));
            }

            return view;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtil.setUserPreferredTheme(this);
        setContentView(R.layout.activity_notification_filter);
        appDatabase = new AppDatabase(NotificationFilterActivity.this, false);

        new Thread(new Runnable() {
            @Override
            public void run() {

                PackageManager packageManager = NotificationFilterActivity.this.getPackageManager();
                List<ApplicationInfo> appList = packageManager.getInstalledApplications(0);
                int count = appList.size();

                apps = new AppListInfo[count];
                for (int i = 0; i < count; i++) {
                    ApplicationInfo appInfo = appList.get(i);
                    apps[i] = new AppListInfo();
                    apps[i].pkg = appInfo.packageName;
                    apps[i].name = appInfo.loadLabel(packageManager).toString();
                    apps[i].icon = NotificationFilterActivity.this.resizeIcon(appInfo.loadIcon(packageManager), 48);
                    apps[i].isEnabled = appDatabase.isEnabled(appInfo.packageName);
                }

                Arrays.sort(apps, new Comparator<AppListInfo>() {
                    @Override
                    public int compare(AppListInfo lhs, AppListInfo rhs) {
                        return lhs.name.compareToIgnoreCase(rhs.name);
                    }
                });

//                NotificationFilterActivity.this.runOnUiThread(() -> NotificationFilterActivity.this.displayAppList());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayAppList();
                    }
                });
            }
        }).start();

    }

    private void displayAppList() {

        listView = findViewById(R.id.lvFilterApps);
        AppListAdapter adapter = new AppListAdapter();
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setLongClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                if (i == 0) {

                    boolean enabled = listView.isItemChecked(0);
                    for (int j = 0; j < apps.length; j++) {
                        listView.setItemChecked(j, enabled);
                    }
                    appDatabase.setAllEnabled(enabled);
                } else {
                    boolean checked = listView.isItemChecked(i);
                    appDatabase.setEnabled(apps[i - 1].pkg, checked);
                    apps[i - 1].isEnabled = checked;
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {
                if (i == 0)
                    return true;
                final Context context = NotificationFilterActivity.this;
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View mView = NotificationFilterActivity.this.getLayoutInflater().inflate(R.layout.popup_notificationsfilter, null);
                builder.setMessage(context.getResources().getString(R.string.extra_options));

                ListView lv = mView.findViewById(R.id.extra_options_list);
                final String[] options = new String[]{
                        context.getResources().getString(R.string.privacy_options)
                };
                ArrayAdapter<String> extra_options_adapter = new ArrayAdapter<>(NotificationFilterActivity.this,
                        android.R.layout.simple_list_item_1, options);
                lv.setAdapter(extra_options_adapter);
                builder.setView(mView);

                final AlertDialog ad = builder.create();

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> new_adapterView, View new_view, int new_i, long new_l) {
                        switch (new_i) {
                            case 0:
                                AlertDialog.Builder myBuilder = new AlertDialog.Builder(context);
                                final String packageName = apps[i - 1].pkg;

                                View myView = NotificationFilterActivity.this.getLayoutInflater().inflate(R.layout.privacy_options, null);
                                CheckBox checkbox_contents = myView.findViewById(R.id.checkbox_contents);
                                checkbox_contents.setChecked(appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_CONTENTS));
                                checkbox_contents.setText(context.getResources().getString(R.string.block_contents));
                                CheckBox checkbox_images = myView.findViewById(R.id.checkbox_images);
                                checkbox_images.setChecked(appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES));
                                checkbox_images.setText(context.getResources().getString(R.string.block_images));

                                myBuilder.setView(myView);
                                myBuilder.setTitle(context.getResources().getString(R.string.privacy_options));
                                myBuilder.setPositiveButton(context.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.dismiss();
                                    }
                                });
                                myBuilder.setMessage(context.getResources().getString(R.string.set_privacy_options));

                                checkbox_contents.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                        appDatabase.setPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_CONTENTS,
                                                compoundButton.isChecked());
                                    }
                                });
                                checkbox_images.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                        appDatabase.setPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES,
                                                compoundButton.isChecked());
                                    }
                                });

                                ad.cancel();
                                myBuilder.show();
                                break;
                        }
                    }
                });

                ad.show();
                return true;
            }
        });

        listView.setItemChecked(0, appDatabase.getAllEnabled()); //"Select all" button
        for (int i = 0; i < apps.length; i++) {
            listView.setItemChecked(i + 1, apps[i].isEnabled);
        }

        listView.setVisibility(View.VISIBLE);
        findViewById(R.id.spinner).setVisibility(View.GONE);

    }

    private Drawable resizeIcon(Drawable icon, int maxSize) {
        Resources res = getResources();

        //Convert to display pixels
        maxSize = (int) (maxSize * res.getDisplayMetrics().density);

        Bitmap bitmap = Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        icon.draw(canvas);

        return new BitmapDrawable(res, bitmap);

    }
}
