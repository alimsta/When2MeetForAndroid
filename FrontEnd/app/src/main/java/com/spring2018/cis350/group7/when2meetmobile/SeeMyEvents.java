package com.spring2018.cis350.group7.when2meetmobile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * SeeMyEvents allows the user to see a list of their events, with different views for
 * guest and admin permission
 */

public class SeeMyEvents  extends AppCompatActivity {
    public ArrayList<String> listOfEvents = new ArrayList<>();
    public ArrayList<String> eventNames = new ArrayList<>();
    ArrayAdapter<String> adapter;
    ListView list;
    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent creatingIntent = getIntent();
        final String eventView = creatingIntent.getStringExtra("view");
        String url = "";
        if (eventView.equals("guest")) {
            setContentView(R.layout.activity_see_my_guest_events);
            url = "http://10.0.3.2:3000/getGuestEvents";
        } else {
            setContentView(R.layout.activity_see_my_admin_events);
            url = "http://10.0.3.2:3000/getAdminEvents";
        }

        queue = Volley.newRequestQueue(this);
        StringRequest requestAdminEvents = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            System.out.println(responseObject.toString());
                            JSONArray eventArray = null;
                            if (eventView.equals("guest")) {
                                eventArray = responseObject.getJSONArray("accepted");
                                for(int i = 0 ; i < eventArray.length(); i++) {
                                    JSONObject eventObject = eventArray.getJSONObject(i);
                                    listOfEvents.add(eventObject.toString());
                                    eventNames.add(eventObject.getString("name"));
                                }
                            } else {
                                eventArray = responseObject.getJSONArray("events");
                                for(int i = 0 ; i < eventArray.length(); i++) {
                                    JSONObject eventObject = eventArray.getJSONObject(i);
                                    listOfEvents.add(eventObject.toString());
                                    eventNames.add(eventObject.getString("eventName"));
                                }
                            }

                            // set the onclick listener for each event
                            specificEventClick();

                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error: " +
                                    e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
                System.out.println(new String (error.getMessage().getBytes()));
                System.out.print(error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
                final String token = appPreferences.getString("token", "");
                if("".equals(token)) {
                    generateErrorAndReturn("Invalid Token, redirecting to login");
                }
                params.put("jwt", token);
                return params;
            }
        };
        queue.add(requestAdminEvents);
    }

    // Open the specific event page to an event upon user click.
    private void specificEventClick() {
        list = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter<String>(SeeMyEvents.this,
                android.R.layout.simple_list_item_1, eventNames);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = listOfEvents.get(position);
                try {
                    JSONObject target = new JSONObject(name);
                    int eventId = target.getInt("identifier");
                    Intent goToEvent = new Intent(SeeMyEvents.this, EventPage.class);
                    goToEvent.putExtra("eventID", eventId);
                    startActivity(goToEvent);
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), "Error: " +
                            e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void generateErrorAndReturn(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                Intent goToLogin = new Intent(SeeMyEvents.this, SignInActivity.class);
                startActivity(goToLogin);
            }
        }.start();
    }

}
