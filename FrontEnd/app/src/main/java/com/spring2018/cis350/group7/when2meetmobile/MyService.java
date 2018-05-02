package com.spring2018.cis350.group7.when2meetmobile;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * An IntentService that makes a call to the database. If a user has "messages", then these messages
 * will be output to a notif sent to the user e.g. an invite to an event.
 *
 * Created by anniesu on 3/21/18.
 */

public class MyService extends IntentService {
    RequestQueue queue;

    public MyService() {
        super("MyServiceName");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("MyService", "About to execute MyTask");
        new MyTask().execute();
        //this.sendNotification(this);
    }
    private class MyTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            Log.d("MyService - MyTask", "Calling doInBackground within MyTask");
            queue = Volley.newRequestQueue(getApplicationContext());

            // TODO - fill in background task

            SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", 0);
            final String token = appPreferences.getString("token", "");


            final String url = "http://10.0.3.2:3000/getUserMessages";
            StringRequest messagesRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject responseObject = new JSONObject(response);
                                JSONArray msgArray = responseObject.getJSONArray("messages");
                                for (int i = 0; i < msgArray.length(); i++) {
                                    JSONObject jsonobject = msgArray.getJSONObject(i);
                                    String message = jsonobject.getString("message");
                                    String eventIdentifier = jsonobject.getString("eventIdentifier");
                                    Log.v("message", message);
                                    Log.v("event identifier", eventIdentifier);

                                    // Create notif
                                    Context context = MyService.this;
                                    Intent notificationIntent;

                                    // Set on click for notif to redirect to event page/invite page
                                    if (message.contains("invited to")) {
                                        notificationIntent = new Intent(context, InvitePage.class);
                                    }
                                    else {
                                        notificationIntent = new Intent(context, EventPage.class);
                                    }
                                    int eventIdentifierInt = Integer.parseInt(eventIdentifier);
                                    Log.v("eventIdentifier as int", eventIdentifier + "");
                                    notificationIntent.putExtra("eventID", eventIdentifierInt);
                                    PendingIntent contentIntent = PendingIntent.getActivity(context,
                                            0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                                    NotificationManager notificationMgr =
                                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                                    Notification.Builder builder = new Notification.Builder(context);
                                    builder.setContentTitle("when2meet Notification");
                                    builder.setContentText(message);
                                    builder.setSmallIcon(android.R.drawable.star_on);
                                    builder.setContentIntent(contentIntent);
                                    builder.setAutoCancel(true);
                                    Notification myNotification = builder.build();
                                    if (notificationMgr != null) {
                                        Log.v("MyService", "notification manager valid");
                                        Log.v("event identifier", eventIdentifier);
                                        int randomInt = generateRandomInt();
                                        Log.v("random int 4 notif", randomInt + "");
                                        notificationMgr.notify(randomInt, myNotification);
                                    }
                                }
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Error: " +
                                        e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println(error.toString());
                    System.out.println(Arrays.toString(error.networkResponse.data));
                    System.out.print(error.getMessage());
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("jwt", token);
                    return params;
                }
            };
            queue.add(messagesRequest);
            Log.v("MyService", "finished messages request");
            return false;
        }
    }

    // Send a notification
    @SuppressLint("NewApi")
    private void sendNotification(Context context) {
        Intent notificationIntent = new Intent(context, MainMenu.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        NotificationManager notificationMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification =  new Notification(android.R.drawable.star_on, "Refresh", System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notification.setLatestEventInfo(context, "Title","Content", contentIntent);

        // Check for boolean that lets you know if there are messages to send out as notifs
        SharedPreferences sp = getSharedPreferences("when2meetPreferences", 0);
        if (sp.getBoolean("userHasMessages", false)) {
            String userMessages = sp.getString("userMessages", "DEFAULT");
            Log.v("User messages from sp", userMessages);

            String[] splitMessages = userMessages.split(",");
            if (splitMessages.length == 1)  {
                // Only issue one notification
                String messageToOutput = splitMessages[0].substring(1, splitMessages[0].length() - 1);
                Notification.Builder builder = new Notification.Builder(this);
                builder.setContentTitle("when2meet Notification");
                builder.setContentText(messageToOutput); // TODO - fill in this string correctly.
                builder.setSmallIcon(android.R.drawable.star_on);
                Notification myNotification = builder.build();
                if (notificationMgr != null) {
                    Log.v("MyService", "notification manager valid");
                    notificationMgr.notify(0, myNotification);
                }
            }
            if (splitMessages.length > 1) {
                // Send each notification/message stored in the user's messages field
                for (int i = 0; i < splitMessages.length; i++) {
                    String msg = splitMessages[i];
                    Log.v("notification[i]", msg);
                    Notification.Builder builder = new Notification.Builder(this);
                    builder.setContentTitle("when2meet Notification");
                    builder.setContentText(msg);
                    builder.setSmallIcon(android.R.drawable.star_on);
                    Notification myNotification = builder.build();
                    if (notificationMgr != null) {
                        Log.v("MyService", "notification manager valid");
                        notificationMgr.notify(generateRandomInt(), myNotification);
                    }
                }
            }
            sp.edit().putBoolean("userHasMessages", false).commit();

            // TODO - logic for removing messages from the messages of the user
        }
    }

    /**
     * Create a unique integer to be used as the notification id (this is so multiple notifications
     * can be displayed.
     */
    private int generateRandomInt() {
        //int randomInt = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
        Random random = new Random();
        int randomInt = random.nextInt(9999 - 1000) + 1000;
        return randomInt;
    }
}
