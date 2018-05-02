package com.spring2018.cis350.group7.when2meetmobile;
/**
 * MainMenu is the activity that is displayed as the main navigation menu. It contains the
 * hamburger menu, create event, my guest events, my admin events, and reset password.
 */

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.Calendar;
import java.util.TimeZone;

public class MainMenu extends AppCompatActivity {

    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    private LinearLayout left_drawer;
    private DrawerLayout mDrawerLayout;
    private String mActivityTitle;

    private static final long REPEAT_TIME = 1000 * 30;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        setTitle("What would you like to do?");
        mDrawerList = (ListView)findViewById(R.id.navList);
        left_drawer = findViewById(R.id.left_drawer);
        addDrawerItems();
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();
        setRecurringAlarm(this);
    }

    // Start CreateEvent activity when "Create event" button is clicked
    public void onCreateEventClick(View view) {
        Intent i = new Intent(this, CreateEvent.class);
        startActivity(i);
    }

    // adds buttons to hamburger menu
    private void addDrawerItems() {
        String[] osArray = {"Search", "Main Menu", "Password Reset", "Log Out"};
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, osArray);
        mDrawerList.setAdapter(mAdapter);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: //Search
                        startActivity(new Intent(getApplicationContext(), SearchActivity.class));
                        break;
                    case 1: //Main Menu
                        startActivity(new Intent(getApplicationContext(), MainMenu.class));
                        break;
                    case 2: //Reset Password
                        startActivity(new Intent(getApplicationContext(), ResetPassword.class));
                        break;
                    case 3: //Log Out
                        SharedPreferences appPref = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = appPref.edit();
                        editor.putString("token", "").apply();
                        editor.putBoolean("logged", false).apply();
                        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                        break;
                    default:
                        break;
                }
            }
        });
    }


    // create an action bar button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.actionbarbutton) {
            if (mDrawerLayout.isDrawerOpen(left_drawer)) {
                mDrawerLayout.closeDrawer(left_drawer);

            }
            else {
                mDrawerLayout.openDrawer(left_drawer);
            }

        }
        return super.onOptionsItemSelected(item);
    }

    // Open up guest event page
    public void onGuestEventsClick(View view) {
        Intent i = new Intent(this, SeeMyEvents.class);
        i.putExtra("view", "guest");
        startActivity(i);
    }

    // Open up admin events page
    public void onAdminEventsClick(View view) {
        Intent i = new Intent(this, SeeMyEvents.class);
        i.putExtra("view", "admin");
        startActivity(i);
    }

    // Set an alarm that recurs every 1 minute (used for checking for notifs)
    private void setRecurringAlarm(Context context) {

        Calendar updateTime = Calendar.getInstance();
        updateTime.setTimeZone(TimeZone.getDefault());
        updateTime.set(Calendar.HOUR_OF_DAY, 12);
        updateTime.set(Calendar.MINUTE, 30);
        Intent downloader = new Intent(context, MyStartServiceReceiver.class);
        downloader.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                downloader,       PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 60 * 1000,
                60 * 1000, pendingIntent);
        Log.d("MyActivity", "Set alarmManager.setRepeating to: " + updateTime.getTime().toLocaleString());

    }
}
