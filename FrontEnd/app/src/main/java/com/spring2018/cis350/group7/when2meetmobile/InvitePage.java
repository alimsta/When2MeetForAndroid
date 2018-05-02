package com.spring2018.cis350.group7.when2meetmobile;
/**
 * InvitePage is the activity that allows a user to accept or decline an event invite
 * after receiving the invite notification.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class InvitePage extends AppCompatActivity {
    private RequestQueue queue;
    private int targetEventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_page);

        Intent creatingIntent = getIntent();
        final int eventIDFromIntent = creatingIntent.getIntExtra("eventID", -1);
        if (eventIDFromIntent == -1) {
            generateErrorAndReturn("No Event ID passed in");
        }
        targetEventID = eventIDFromIntent;
        queue = Volley.newRequestQueue(this);

        // Initialize textview
        final TextView eventName = (TextView) findViewById(R.id.invite_page_event_name);

        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        final String token = appPreferences.getString("token", "");
        Log.v("user token", token);
        if ("".equals(token)) {
            generateErrorAndReturn("Invalid Token, redirecting to login");
        }

        final String url = "http://10.0.3.2:3000/event";
        StringRequest getEventInformationRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            String eventNameString = responseObject.getString("evName");
                            eventName.setText(eventNameString);

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
                params.put("eventId", "" + targetEventID);
                params.put("jwt", token);
                return params;

            }
        };
        queue.add(getEventInformationRequest);
    }

    // When the user clicks the "Accept" button for the event, update database accordingly
    public void onAcceptClick(View view) {
        final Intent goToEventPage = new Intent(InvitePage.this, EventPage.class);
        goToEventPage.putExtra("eventID", targetEventID);

        // Add user to the accepted list of the event.
        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        final String token = appPreferences.getString("token", "");
        Log.v("user token", token);
        if ("".equals(token)) {
            generateErrorAndReturn("Invalid Token, redirecting to login");
        }
        final String url = "http://10.0.3.2:3000/acceptInvite";
        StringRequest acceptInviteRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseObject = new JSONObject(response); // This is useless
                            String result = responseObject.getString("result");
                            Log.v("InvitePage: response", result); // TODO - delete print statement
                            if (result.equals("Success")) {
                                //startActivity(goToEventPage);
                                Log.v("InvitePage, accept result", "equals Success!");
                                generateSuccessfulInviteToast("Successfully accepted event invite! " +
                                        "Redirecting to event page...");
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
                params.put("eventID", "" + targetEventID);
                params.put("jwt", token);
                return params;

            }
        };
        queue.add(acceptInviteRequest);
    }

    // When user clicks the "Decline" button, update the database to reflect the user's rejection.
    public void onDeclineClick(View view) {
        // Add user to the declined list of the event
        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        final String token = appPreferences.getString("token", "");
        Log.v("user token", token);
        if ("".equals(token)) {
            generateErrorAndReturn("Invalid Token, redirecting to login");
        }
        final String url = "http://10.0.3.2:3000/declineInvite";
        StringRequest declineInviteRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            String result = responseObject.getString("result");
                            if (result.equals("Success")) {
                                Log.v("InvitePage, decline result", "equals Success!");
                                generateSuccessfulDeclineToast("Declined event invite. " +
                                        "Redirecting to main menu...");
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
                params.put("eventId", "" + targetEventID);
                params.put("jwt", token);
                return params;

            }
        };
        queue.add(declineInviteRequest);
    }


    // Create a generic error message in the form of a Toast
    private void generateErrorAndReturn(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                Intent goToLogin = new Intent(InvitePage.this, SignInActivity.class);
                startActivity(goToLogin);
            }
        }.start();
    }

    // Create a success Toast for a successful invite
    private void generateSuccessfulInviteToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                final Intent goToEventPage = new Intent(InvitePage.this, EventPage.class);
                goToEventPage.putExtra("eventID", targetEventID);
                startActivity(goToEventPage);
            }
        }.start();
    }

    // Create a decline Toast for a successful decline of the event invite.
    private void generateSuccessfulDeclineToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                final Intent goToMainMenu = new Intent(InvitePage.this, MainMenu.class);
                startActivity(goToMainMenu);
            }
        }.start();
    }


}
