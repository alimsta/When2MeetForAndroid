package com.spring2018.cis350.group7.when2meetmobile;
/**
 * EventPage is the activity that allows for a user to view an event, either as an admin or guest.
 * Based on the user access permissions, the user can invite other guests and invite other admins.
 * The user can also input responses and view the responses of all other guests in the event.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.MutableDateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventPage extends AppCompatActivity {

    private RequestQueue queue;
    private int targetEventID;

    private int startTime;
    private int endTime;
    private boolean specificDates;
    private Date startDate;
    private Date endDate;

    private int targetStartTime;
    private int targetStartDate;

    private JSONArray responses;
    private boolean viewingMyRes;
    private boolean[][] myResponses;
    private LinkedList<String>[][] groupResponses;
    private String[] dateTitles;
    private String[] timeTitles;
    private int maxNumberOfResponses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent creatingIntent = getIntent();
        final int eventIDFromIntent = creatingIntent.getIntExtra("eventID", -1);
        if(eventIDFromIntent == -1) {
           generateErrorAndReturn("No Event ID passed in");
        }
        targetEventID = eventIDFromIntent;
        targetStartDate = 0;
        targetStartTime = 0;
        maxNumberOfResponses = 0;
        queue = Volley.newRequestQueue(this);
        viewingMyRes = true;
        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        final String token = appPreferences.getString("token", "");
        Log.v("user token", token);
        if("".equals(token)) {
            generateErrorAndReturn("Invalid Token, redirecting to login");
        }

        final String url = "http://10.0.3.2:3000/event";
        StringRequest getEventInformationRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        processResponseObject(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
                System.out.println(new String(error.networkResponse.data));
                System.out.print(error.getMessage());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("eventId", "" +  targetEventID);
                params.put("jwt", token);
                return params;

            }
        };
        queue.add(getEventInformationRequest);

    }

    // add viewingMyRes if check to account for pop up
    //
    public View.OnClickListener getTouchListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("Event: ", "click");
                String tag = view.getTag().toString();
                String[] tagInts = tag.split("");
                int[] coords = getTableIndexByView(Integer.parseInt(tagInts[2]),
                        Integer.parseInt(tagInts[1]));
                if (viewingMyRes) {
                    Log.v("Coords: ", "" + coords[0] + coords[1]);
                    myResponses[coords[0]][coords[1]] = !myResponses[coords[0]][coords[1]];
                    populate();
                } else {
                    LinkedList<String> targetList = groupResponses[coords[0]][coords[1]];
                    StringBuilder respondantStringBuilder = new StringBuilder();
                    for (String target: targetList) {
                        respondantStringBuilder.append(target).append('\n');
                    }
                    String bodyString = respondantStringBuilder.toString();
                    AlertDialog.Builder builder = new AlertDialog.Builder(EventPage.this);
                    builder.setTitle("Users");
                    builder.setMessage(bodyString);
                    builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        };
    }


    /**
     * Parses JSON Object returned from the "event" request (which returns all event info)
     * @param response
     */
    private void processResponseObject(String response) {
        try {
            JSONObject responseObject = new JSONObject(response);
            System.out.println(response);
            Boolean isOwner = responseObject.getBoolean("isOwner");
            Boolean isAdmin = responseObject.getBoolean("admin");
            if(isOwner) {
                setContentView(R.layout.activity_event_page_owner);
            }
            else if(isAdmin) {
                setContentView(R.layout.activity_event_page_admin);
            }
            else {
                setContentView(R.layout.activity_event_page);
            }
            String eventNameString = responseObject.getString("evName");
            TextView eventName = (TextView) findViewById(R.id.event_name_text);
            eventName.setText(eventNameString);
            responses = responseObject.getJSONArray("responses");
            String eventOwnerString = responseObject.getString("adminName");
            TextView ownerName = (TextView) findViewById(R.id.event_owner);
            ownerName.setText(eventOwnerString);
            this.specificDates = responseObject.getBoolean("specificDates");
            JSONArray timeRanges = responseObject.getJSONArray("timeRange");
            startTime = Integer.parseInt(timeRanges.getString(0));
            endTime = Integer.parseInt(timeRanges.getString(1));
            Log.v("Times: ",startTime + "   :::   " + endTime);
            JSONArray dateRange = responseObject.getJSONArray("dateRange");
            int daysBetween;
            if(specificDates) {
                startDate = new Date(dateRange.getLong(0));
                endDate = new Date(dateRange.getLong(1));
                daysBetween = dayDistance(startDate, endDate);
            }
            else {
                startDate = new Date(dateRange.getLong(0));
                daysBetween = dateRange.length();
            }
            int minutesBetween = getMinutesBetween(startTime, endTime);
            int numberOfTimeSlots = (minutesBetween / 30);
            this.timeTitles = getTimeLabels(startTime, numberOfTimeSlots);
            Log.v("Time Title", Arrays.toString(timeTitles));
            this.dateTitles = getDateLabels(dateRange, specificDates, daysBetween);
            Log.v("Date Title", Arrays.toString(dateTitles));
            myResponses = new boolean[daysBetween][numberOfTimeSlots];
            groupResponses = new LinkedList[daysBetween][numberOfTimeSlots];
            for (int i = 0; i < daysBetween; i++) {
                for (int j = 0; j < numberOfTimeSlots; j++) {
                    groupResponses[i][j] = new LinkedList<String>();
                }
            }
            JSONObject myResponses = responseObject.getJSONObject("personalResponses");
            if(!"".equals(myResponses.get("responder"))) {
                System.out.println("RESPONDED");
                processPersonalResponses(myResponses);
            }
            for(int i = 0; i < 10; i++) {
                for(int j = 0; j < 4; j++) {
                    TextView targetView = (findViewById(R.id.tableLayout3)).findViewWithTag("" + i + "" + j);
                    targetView.setOnClickListener(getTouchListener());
                }
            }
            for(int i = 0; i < this.myResponses.length; i++) {
                System.out.println(Arrays.toString(this.myResponses[i]));
            }
            populate();
        } catch (JSONException |ParseException e) {
            Log.e("Parse Error: ", "error: ", e);
            Toast.makeText(getApplicationContext(), "Error: " +
                    e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void processPersonalResponses(JSONObject myResponses) throws JSONException, ParseException {
        JSONArray timesArr = myResponses.getJSONArray("times");
        for(int i = 0; i < timesArr.length(); i++) {
            JSONObject dateEntry = timesArr.getJSONObject(i);
            System.out.println(dateEntry.toString());
            if(specificDates) {
                int targetColumn = daysBetween(new Date(dateEntry.getLong("day")), startDate);
                System.out.println("TARGETCOLUMN:::: " + targetColumn);
                System.out.println("DATE::: " + (startDate).toString());
                System.out.println("DATE::: " + (new Date(dateEntry.getLong("day"))).toString());
                JSONArray timeRange = dateEntry.getJSONArray("timeRange");
                int[] intArr = new int[timeRange.length()];
                for (int j = 0; j < intArr.length; j++) {
                    intArr[j] = Integer.parseInt(timeRange.getString(j));
                }
                for(int j = 0; j < intArr.length; j++) {
                    int position = getMinutesBetween(startTime, intArr[j]) / 30;
                    this.myResponses[targetColumn][position] = true;
                }
            }
            else {
                DateTime targetDate = new DateTime(new Date(dateEntry.getLong("day")));
                int targetColumn = -1;
                int month = targetDate.monthOfYear().get();
                String addToMonth = month < 10?"0":"";
                int day = targetDate.dayOfMonth().get();
                String addToDay = day < 10?"0":"";
                addToMonth += month;
                addToDay += day;
                System.out.println(dateToDay("" + addToMonth  + "/" + addToDay +
                        "/" + targetDate.year().get()));
                System.out.println(Arrays.toString(dateTitles));
                for(int j = 0; j < dateTitles.length; j++) {
                    if(dateTitles[j].equals(dateToDay("" + addToMonth  + "/" + addToDay +
                            "/" + targetDate.year().get()))) {
                        targetColumn = j;
                    }
                }
                if(targetColumn != -1) {
                    JSONArray timeRange = dateEntry.getJSONArray("timeRange");
                    int[] intArr = new int[timeRange.length()];
                    for(int k = 0; k < timeRange.length(); k++) {
                        intArr[k] = Integer.parseInt(timeRange.getString(k));
                    }
                    System.out.println(Arrays.toString(intArr));
                    for(int j = 0; j < intArr.length; j++) {
                        int position = getMinutesBetween(startTime, intArr[j]) / 30;
                        System.out.println(position);
                        this.myResponses[targetColumn][position] = true;
                    }
                }
                else {
                    System.out.println("NO COLUMN");

                }
            }
        }
    }

    // Generate the labels for the date columns as an array
    private String[] getDateLabels(JSONArray dateRange, boolean specificDates, int size) throws JSONException {
        if(specificDates) {
            MutableDateTime targetDate = new MutableDateTime(startDate);
            String[] retArr = new String[size];
            for(int i = 0; i < size; i++) {
                String label = "" + targetDate.monthOfYear().get() + "/" + targetDate.dayOfMonth().get();
                retArr[i] = label;
                targetDate.addDays(1);
            }
            return retArr;
        }
        else {
            String[] retArr = new String[size];
            for(int i = 0; i < size; i++) {
                DateTime targetDate = new DateTime(new Date(dateRange.getLong(i)));
                String label = dateToDay("0" + targetDate.monthOfYear().get() + "/" + targetDate.dayOfMonth().getAsShortText() +
                                         "/" + targetDate.year().getAsShortText());
                retArr[i] = label;
            }
            return retArr;
        }
    }

    // Convert a specific day back to a day of the week for non-date-specific events
    private String dateToDay(String day) {
        Log.v("Date to Day: ", day);
        if("02/18/2018".equals(day)) {
            return ("Sun.   ");
        }
        else if("02/19/2018".equals(day)){
            return ("Mon.   ");
        }
        else if("02/20/2018".equals(day)){
            return ("Tue.  ");
        }
        else if("02/21/2018".equals(day)){
            return ("Wed.   ");
        }
        else if("02/22/2018".equals(day)){
            return ("Thu. ");
        }
        else if("02/23/2018".equals(day)){
            return ("Fri.   ");
        }
        else {
            return ("Sat.   ");
        }
    }

    // Generates a list of time labels based off of start time and the numbeer of slots
    private String[] getTimeLabels(int startTime, int sizeCount) throws ParseException{
        SimpleDateFormat milTime = new SimpleDateFormat("hhmm");
        MutableDateTime startTimeDate = new MutableDateTime(milTime.parse(String.format("%4s", "" +
                                                        startTime).replace(' ', '0')));
        String[] labels = new String[sizeCount];
        for(int i = 0; i < sizeCount; i++) {
            String label = "" + startTimeDate.hourOfDay().get() + "" + String.format("%2s", "" +
                    startTimeDate.minuteOfDay().get() % 60).replace(' ', '0');
            labels[i] = String.format("%8s", label);
            startTimeDate.addMinutes(30);
        }
        return labels;
    }

    // calculates the number of minutes between two military times
    private int getMinutesBetween(int startTime, int endTime) throws ParseException {
        SimpleDateFormat milTime = new SimpleDateFormat("hhmm");
        Date startTimeDate = milTime.parse(String.format("%4s", "" + startTime).replace(' ', '0'));
        Date endTimeDate = milTime.parse(String.format("%4s", "" + endTime).replace(' ', '0'));
        return Math.abs(Minutes.minutesBetween(new DateTime(startTimeDate), new DateTime(endTimeDate)).getMinutes());
    }

    // Calculates the number of days between two dates
    private int dayDistance(Date startDate, Date endDate) {
        Calendar cal1 = new GregorianCalendar();
        Calendar cal2 = new GregorianCalendar();
        cal1.setTime(startDate);
        cal2.setTime(endDate);
        return daysBetween(cal1.getTime(), cal2.getTime());
    }

    /**
     *  Converts from millis between to days between
     */
    private int daysBetween(Date d1, Date d2){
        int days = (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
        if(days < 0) return -1 * days;
        return days;
    }

    /**
     * Invite (pre-existing) users to the event and store to database
     * @param view
     */
    public void onInviteButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(EventPage.this);
        builder.setTitle("Users to invite, separated by commas");
        final EditText userTargets = new EditText(EventPage.this);
        builder.setView(userTargets);
        builder.setPositiveButton("INVITE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                String requestPeople = userTargets.getText().toString();
                if(requestPeople.isEmpty()) {
                    Toast.makeText(EventPage.this, "No users invited", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
                final String[] arrayInvites = requestPeople.split(",");
                for (int invite = 0; invite < arrayInvites.length; invite++) {
                    arrayInvites[invite] = arrayInvites[invite].trim();
                }

                //StringRequest inviteRequest = createInviteRequest();
               StringRequest inviteRequest = new StringRequest(Request.Method.POST,
                        "http://10.0.3.2:3000/invite",
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    JSONObject responseObject = new JSONObject(response);
                                    String wasError = responseObject.getString("error");
                                    if("none".equals(wasError)) {
                                        Toast.makeText(getApplicationContext(),
                                                "Successfully added all users", Toast.LENGTH_SHORT).show();
                                        new CountDownTimer(2000, 2000) {
                                            @Override
                                            public void onTick(long l) {
                                            }
                                            @Override
                                            public void onFinish() {
                                                dialogInterface.dismiss();
                                            }
                                        }.start();
                                    }
                                    else {
                                        Toast.makeText(getApplicationContext(),
                                                "Error: " + wasError, Toast.LENGTH_SHORT).show();
                                        new CountDownTimer(2000, 2000) {
                                            @Override
                                            public void onTick(long l) {
                                            }
                                            @Override
                                            public void onFinish() {
                                                dialogInterface.dismiss();
                                            }
                                        }.start();
                                    }
                                } catch (JSONException e) {
                                    Toast.makeText(getApplicationContext(), "Error: " +
                                            e.toString(), Toast.LENGTH_LONG).show();
                                    new CountDownTimer(2000, 2000) {
                                        @Override
                                        public void onTick(long l) {
                                        }
                                        @Override
                                        public void onFinish() {
                                            dialogInterface.dismiss();
                                        }
                                    }.start();
                                }
                            }
                        },  new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println(error.toString());
                        System.out.println(Arrays.toString(error.networkResponse.data));
                        System.out.print(error.getMessage());
                        new CountDownTimer(2000, 2000) {
                            @Override
                            public void onTick(long l) {
                            }
                            @Override
                            public void onFinish() {
                                dialogInterface.dismiss();
                            }
                        }.start();
                    }
                }) {
                    @Override
                    protected Map<String, String> getParams() {
                        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", 0);
                        final String token = appPreferences.getString("token", "");
                        if("".equals(token)) {
                            generateErrorAndReturn("Invalid Token, redirecting to login");
                        }
                        Map<String, String> params = new HashMap<>();
                        params.put("invitedUsers", Arrays.toString(arrayInvites));
                        params.put("eventId", "" + targetEventID);
                        params.put("jwt", token);
                        return params;
                    }
                };
                queue.add(inviteRequest);
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private StringRequest createInviteRequest() {
        return null;
    }

    /**
     * When invite nonusers button is clicked, display a text pop to input an email. This email will
     * be sent a link to download the application.
      */
    public void onInviteNonusersButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Invite a Nonuser via Email");
        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String inputEmail = input.getText().toString();
                final String url = "http://10.0.3.2:3000/inviteNonUser";
                SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", 0);
                final String token = appPreferences.getString("token", "");
                StringRequest usersRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (response.equals("false")) {
                                    System.out.println("false");
                                    Toast.makeText(getApplicationContext(),
                                            "Could not send email to nonuser.", Toast.LENGTH_SHORT).show();
                                } else {
                                    System.out.println("true");
                                    Toast.makeText(getApplicationContext(),
                                            "Email sent!", Toast.LENGTH_SHORT).show();
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
                        params.put("email", inputEmail);
                        params.put("jwt", token);
                        return params;
                    }
                };
                queue.add(usersRequest);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    /**
     * Display a pop-up of currently RSVP'd guests
      */
    public void onViewGuestsButtonClick(View view) {
        final String token = checkToken();

        final String url = "http://10.0.3.2:3000/users";
        StringRequest usersRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject responseObject = new JSONObject(response);
                            JSONArray acceptedUsers = responseObject.getJSONArray("accepted");
                            JSONArray rejectedUsers = responseObject.getJSONArray("rejected");
                            int numResponses = acceptedUsers.length() + rejectedUsers.length();
                            JSONArray invitedUsers = responseObject.getJSONArray("invited");
                            int totalUsers = acceptedUsers.length() + rejectedUsers.length() + invitedUsers.length();
                            StringBuilder userMessage = new StringBuilder();
                            userMessage.append("Total Responses: " + numResponses + "\n");
                            userMessage.append("Total Invites: " + totalUsers + "\n" + "\n");
                            System.out.println("here");
                            System.out.println(acceptedUsers.toString());
                            userMessage.append("Accepted Guests are: \n");
                            for(int i = 0; i < acceptedUsers.length(); i++) {
                                userMessage.append(acceptedUsers.get(i).toString()).append('\n');
                            }
                            userMessage.append("Invited Guests are: \n");

                            for(int i = 0; i < invitedUsers.length(); i++) {
                                userMessage.append(invitedUsers.get(i).toString() + '\n');
                            }

                            System.out.println(userMessage.toString());
                            AlertDialog.Builder builder = new AlertDialog.Builder(EventPage.this);
                            builder.setTitle("users");
                            builder.setMessage(userMessage.toString());
                            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error: " +
                                    e.toString(), Toast.LENGTH_LONG).show();
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
        queue.add(usersRequest);

    }

    /**
     * Check to see if the saved user token is valid
      */
    public void deleteEventClick(View view){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        final String url = "http://10.0.3.2:3000/deleteEvent";
                        StringRequest deleteEventRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        try {
                                            if (response.equals("Success")) {
                                                Toast.makeText(getApplicationContext(), "Successfully deleted event.",
                                                        Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(getApplicationContext(), MainMenu.class));
                                            }
                                            else {
                                                Toast.makeText(getApplicationContext(), "Event deletion unsuccessful.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        } catch (Exception e) {
                                            Toast.makeText(getApplicationContext(), "Error: " +
                                                    e.toString(), Toast.LENGTH_LONG).show();
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
                                final String token = checkToken();
                                Map<String, String> params = new HashMap<>();
                                params.put("eventID", "" + targetEventID);
                                params.put("jwt", token);
                                return params;
                            }
                        };
                        queue.add(deleteEventRequest);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this event?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    // check to see if the saved token is valid
    @NonNull
    private String checkToken() {
        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", 0);
        final String token = appPreferences.getString("token", "");
        if("".equals(token)) {
            Toast.makeText(getApplicationContext(), "Invalid Token, redirecting to login",
                    Toast.LENGTH_SHORT).show();
            new CountDownTimer(2000, 2000) {
                @Override
                public void onTick(long l) {
                }
                @Override
                public void onFinish() {
                    Intent goToLogin = new Intent(EventPage.this, SignInActivity.class);
                    startActivity(goToLogin);
                }
            }.start();
        }
        return token;
    }

    // Remove user from the event
    public void onLeaveButtonClick(View view) {
        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        final String token = appPreferences.getString("token", "");

        // Alert the user they are about to leave event.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure you want to leave the event?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO - add logic for actually leaving the event.
                final String url = "http://10.0.3.2:3000/removeGuest";
                StringRequest removeRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (response.equals("Success")) {
                                    // Make toast declaring success and redirect to main menu
                                    Toast.makeText(getApplicationContext(), "Successfully left event.",
                                            Toast.LENGTH_SHORT).show();
                                    new CountDownTimer(2000, 2000) {
                                        @Override
                                        public void onTick(long l) {
                                        }
                                        @Override
                                        public void onFinish() {
                                            Intent goToMainMenu = new Intent(EventPage.this, MainMenu.class);
                                            startActivity(goToMainMenu);
                                        }
                                    }.start();
                                }
                                else {
                                    Toast.makeText(getApplicationContext(), "ERROR: could not leave event!",
                                            Toast.LENGTH_SHORT).show();
                                    new CountDownTimer(2000, 2000) {
                                        @Override
                                        public void onTick(long l) {
                                        }
                                        @Override
                                        public void onFinish() {
                                        }
                                    }.start();
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
                        params.put("eventID", "" + targetEventID); // TODO - replace dummy event
                        params.put("jwt", token);
                        return params;
                    }
                };
                queue.add(removeRequest);
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Do nothing
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    /**
     * Generate a Toast that displays a given message on the screen.
     * @param message
     */
    private void generateErrorAndReturn(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) {
            }
            @Override
            public void onFinish() {
                Intent goToLogin = new Intent(EventPage.this, SignInActivity.class);
                startActivity(goToLogin);
            }
        }.start();
    }

    private class ResponseClick implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            TextView textView = (TextView) view;
            String[] tagIndeces = textView.getTag().toString().split("");

        }
    }

    // Based on current state, decide whether to display all user responses
    // or to display just the current user's response
    public void onResponseButtonClick(View view) {
        // if currently viewing your own responses, switch to all responses
        if (viewingMyRes) {
            viewingMyRes = false;
            populateResponseTable(responses);
        } else {
            viewingMyRes = true;
            // TODO display the current user's availability
        }
        populate();

    }

    // Retrieve the responses of the given event for all users
    private void populateResponseTable(JSONArray responses) {
        try {
            int numResponses = responses.length();
            System.out.println("RESPONSES::: " + responses.toString());
            // iterate through all responses of the event, extracting the
            // response times for each invited user
            for (int i = 0; i < numResponses; i++) {
                JSONObject resObj = responses.getJSONObject(i);
                JSONArray timesArr = resObj.getJSONArray("times");
                JSONObject responder = resObj.getJSONObject("responder");
                String name = responder.getString("name");
                for (int k = 0; k < timesArr.length(); k++) {
                    JSONObject dateEntry = timesArr.getJSONObject(k);
                    if (specificDates) {
                        int targetColumn = daysBetween(new Date(dateEntry.getLong("day")), startDate);
                        JSONArray timeRange = dateEntry.getJSONArray("timeRange");
                        int[] intArr = new int[timeRange.length()];
                        for (int l = 0; l < timeRange.length(); l++) {
                            intArr[l] = Integer.parseInt(timeRange.getString(l));
                        }
                        for (int j = 0; j < intArr.length; j++) {
                            int position = getMinutesBetween(startTime, intArr[j]) / 30;
                            System.out.println("COLUMN:::: " + targetColumn);
                            this.groupResponses[targetColumn][position].add(name);
                            if( this.groupResponses[targetColumn][position].size() > maxNumberOfResponses) {
                                maxNumberOfResponses =  this.groupResponses[targetColumn][position].size();
                            }
                        }
                    } else {
                        DateTime targetDate = new DateTime(new Date(dateEntry.getLong("day")));
                        int targetColumn = -1;
                        int month = targetDate.monthOfYear().get();
                        String addToMonth = month < 10?"0":"";
                        int day = targetDate.dayOfMonth().get();
                        String addToDay = day < 10?"0":"";
                        addToMonth += month;
                        addToDay += day;
                        for (int j = 0; j < dateTitles.length; j++) {
                            if(dateTitles[j].equals(dateToDay("" + addToMonth  + "/" + addToDay +
                                    "/" + targetDate.year().get()))) {
                                targetColumn = j;
                            }
                        }
                        if (targetColumn != -1) {
                            JSONArray timeRange = dateEntry.getJSONArray("timeRange");
                            int[] intArr = new int[timeRange.length()];
                            for (int l = 0; l < timeRange.length(); l++) {
                                intArr[l] = Integer.parseInt(timeRange.getString(l));
                            }
                            for(int j = 0; j < intArr.length; j++) {
                                int position = getMinutesBetween(startTime, intArr[j]) / 30;
                                this.groupResponses[targetColumn][position].add(name);
                                if( this.groupResponses[targetColumn][position].size() > maxNumberOfResponses) {
                                    maxNumberOfResponses =  this.groupResponses[targetColumn][position].size();
                                }
                            }
                        }
                        else {
                            System.out.println("NO COLUMN FOUND");
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            Log.e("Parse Error: ", "error: ", e);
        }
    }

    // Determine the color of the time slot
    private int colorMeGreen(int totalRes, int currResNum) {
        int b = 255;
        int r = 255;
        int g = 255;
        if (currResNum > 0) {
            b = 78;
            r = 78;
            int gInterv = (255 - 130) / totalRes;
            g = gInterv * currResNum + 130;
        }
        return Color.rgb(r, g, b);
    }

    // Based on the given TextView, find the corresponding index
    // in the stored response table
    // date corresponds to x val, time corresponds to y val
    private int[] getTableIndexByView(int date, int time) {
        int x = targetStartDate + date;
        int y = targetStartTime + time;
        return new int[]{x, y};
    }

    // Shift the view back by one day
    public void shiftLeft(View view) {
        if (targetStartDate != 0) {
            targetStartDate--;
            populate();
        }
    }

    // Shift the view up by one day
    public void shiftRight(View view) {
        if (targetStartDate + 3 < groupResponses.length - 1) {
            Log.v("Values: ", "" + targetStartDate + " :::: " + (groupResponses.length - 1));
            targetStartDate++;
            populate();
        }
    }

    // Shift the view back by thirty minutes
    public void shiftUp(View view) {
        if (targetStartTime != 0) {
            targetStartTime--;
            populate();
        }
    }

    // Shift the view forward by thrity minutes
    public void shiftDown(View view) {
        if (targetStartTime + 9 < groupResponses[0].length - 1) {
            targetStartTime++;
            populate();
        }
    }


    public void onDragTime(View view) {

    }

    /**
     * Will remind all users who have been invited to and have accepted the event request to
     * update the availability poll via a notification.
     */
    public void onRemindButtonClick(View view) {
        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        final String token = appPreferences.getString("token", "");
        final String url = "http://10.0.3.2:3000/remindUsers";
        StringRequest remindRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("Success")) {
                            Toast.makeText(getApplicationContext(),
                                    "Successfully reminded event guests to fill out availability poll.",
                                    Toast.LENGTH_SHORT).show();
                            new CountDownTimer(2000, 2000) {
                                @Override
                                public void onTick(long l) {
                                }
                                @Override
                                public void onFinish() {
                                }
                            }.start();
                        }
                        else {
                            Toast.makeText(getApplicationContext(), "ERROR: could not remind event guests!",
                                    Toast.LENGTH_SHORT).show();
                            new CountDownTimer(2000, 2000) {
                                @Override
                                public void onTick(long l) {
                                }
                                @Override
                                public void onFinish() {
                                }
                            }.start();
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
        queue.add(remindRequest);

    }

    /**
     * Saves all user input for time availabilities for the event and bundles it into a post request
     * @param view
     */
    public void onSubmitTimeSelections(View view) {

        // Bundle up all the dates selected
        final List<String> responses_list = new ArrayList<String>();
        long startDateMillis = startDate.getTime();
        long day = 86400000;

        for (int i = 0; i < myResponses.length; i++) {
            boolean daySelected = false;
            for (int j = 0; j < myResponses[0].length; j++) {
                long today = startDateMillis + (day * i);

                if (myResponses[i][j]) {
                    // Only add the date title once, before all the hours!
                    if (!daySelected) {
                        daySelected = true;
                        responses_list.add("D" + today);
                    }
                    responses_list.add(timeTitles[j]); // Assuming military time is how the backend is expecting the time to be formatted
                }
            }
        }

        // Post request that delivers user's availabilities to the backend
        SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        final String token = appPreferences.getString("token", "");
        final String url = "http://10.0.3.2:3000/respond";
        StringRequest respondRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.e("Response: ", response);
                        try {
                            if ((new JSONObject(response).getString("error").equals("false"))) {
                                Toast.makeText(getApplicationContext(), "Successfully sent user responses.",
                                        Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(), "ERROR: could not send user responses!",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
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
                params.put("dateRange", Arrays.toString(responses_list.toArray()));
                return params;
            }
        };
        queue.add(respondRequest);
    }

    public void addAdmin(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("INPUT DESIRED ADMIN'S USERNAME");
        final EditText name = new EditText(this);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        name.setLayoutParams(layout);
        builder.setView(name);
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final String adminName = name.getText().toString();
                if("".equals(adminName)) {
                    Toast.makeText(getApplicationContext(),"No name to be made admin", Toast.LENGTH_SHORT).show();
                }
                else {
                    dialogInterface.cancel();
                    final String url = "http://10.0.3.2:3000/addAdmin";
                    StringRequest addNewAdmin = new StringRequest(Request.Method.POST, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    try {
                                        JSONObject confirmation = new JSONObject(response);
                                        boolean success = confirmation.getBoolean("success");
                                        if(success) {
                                            Toast.makeText(getApplicationContext(),"Admin Added", Toast.LENGTH_SHORT).show();
                                        }
                                        else {
                                            String error = confirmation.getString("error");
                                            Toast.makeText(getApplicationContext(),"Error: " + error, Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException e) {
                                        Toast.makeText(getApplicationContext(), e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
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
                        protected Map<String, String> getParams() throws AuthFailureError {
                            HashMap<String, String> params = new HashMap<>();
                            params.put("eventId", "" + targetEventID);
                            SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
                            final String token = appPreferences.getString("token", "");
                            params.put("jwt", token);
                            params.put("targetName", adminName);
                            return params;
                        }
                    };
                    queue.add(addNewAdmin);
                }
            }
        });
        builder.show();
    }
    /**
     * Populate the table values of availability poll with proper dates/times
     */
    private void populate() {
        TableLayout responseTable = (TableLayout) findViewById(R.id.tableLayout3);
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 4; j++) {
                TextView targetView = (TextView) responseTable.findViewWithTag("" + i + "" + j);
                targetView.setBackgroundColor(R.drawable.black);
                targetView.setClickable(false);
                targetView.setText("--|--|--");
            }
        }
        if(viewingMyRes) {
            for(int i = 0; i < myResponses.length - targetStartDate && i < 4; i++) {
                for(int j = 0; j < myResponses[0].length - targetStartTime && j < 10; j++) {
                    int[] coordinates = getTableIndexByView(i, j);
                    String targetViewTag = "" + j + "" + i;
                    Log.e("Text Name: ", targetViewTag);
                    TextView targetView = (TextView) responseTable.findViewWithTag(targetViewTag);
                    targetView.setClickable(true);
                    Log.v("Count: ", "" + (i));
                    Log.v("Count J: ", "" + (j));
                    targetView.setOnClickListener(getTouchListener());
                    if(!myResponses[coordinates[0]][coordinates[1]]) continue;
                    targetView.setBackgroundColor(Color.GREEN);
                }
            }
        }
        else {
            for(int i = 0; i < myResponses.length - targetStartDate && i < 4; i++) {
                for(int j = 0; j < myResponses[0].length - targetStartTime && j < 10; j++) {
                    int[] coordinates = getTableIndexByView(i, j);
                    if(groupResponses[coordinates[0]][coordinates[1]].isEmpty()) continue;
                    String targetViewTag = "" + j + "" + i;
                    Log.e("Text Name: ", targetViewTag);
                    TextView targetView = (TextView) responseTable.findViewWithTag(targetViewTag);
                    targetView.setBackgroundColor(colorMeGreen(maxNumberOfResponses,
                                                               groupResponses[coordinates[0]][coordinates[1]].size()));
                    Log.e("COLOR::: ", "" + colorMeGreen(maxNumberOfResponses,
                            groupResponses[i][j].size()));
                    targetView.setClickable(true);
                }
            }
        }
        for(int i = targetStartDate; i < myResponses.length && (i - targetStartDate) < 4; i++) {
            int dateIndex = i - targetStartDate;
            Log.v("Date Index: ", "" + dateIndex);
            TextView targetView = (TextView) responseTable.findViewWithTag("dateLabel" + dateIndex);
            targetView.setText(dateTitles[i]);
        }
        for(int j = targetStartTime; j < myResponses[0].length && (j - targetStartTime) < 10; j++) {
            int timeIndex = j - targetStartTime;
            TextView targetView = (TextView) responseTable.findViewWithTag("timeLabel" + timeIndex);
            targetView.setText(timeTitles[j]);
        }
    }

    /**
     * Convert a day of the week to a valid date during the year (for backend purposes)
     * @param day
     * @return date as a String representation (so it can be parsed in JS)
     */
    private String dayToDate(String day) {
        if("Sunday".equals(day)) {
            return ("02/18/2018");
        }
        else if("Monday".equals(day)){
            return ("02/19/2018");
        }
        else if("Tuesday".equals(day)){
            return ("02/20/2018");
        }
        else if("Wednesday".equals(day)){
            return ("02/21/2018");
        }
        else if("Thursday".equals(day)){
            return ("02/22/2018");
        }
        else if("Friday".equals(day)){
            return ("02/23/2018");
        }
        else {
            return ("02/24/2018");
        }
    }
}
