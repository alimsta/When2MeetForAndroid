package com.spring2018.cis350.group7.when2meetmobile;
/**
 * CreateEvent is the activity where a user can create an event with desired attributes
 * and invited guests.
*/

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class CreateEvent extends AppCompatActivity {
    RequestQueue queue;
    ToggleButton toggle;
    static final int DATE_RANGE_PICKER_REQUEST = 1;  // request code for DateRangePicker activity
    String startDate = null;
    String endDate = null;
    List<String> daysOfWeekChosen = null;
    Spinner startHourSpinner;
    Spinner endHourSpinner;
    String startHourStr = "600";
    boolean initialStartHourSelected = false;
    String endHourStr = "";
    boolean initialEndHourSelected = false;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        setContentView(R.layout.activity_create_event);
        queue = Volley.newRequestQueue(this);

        // Initialize submit button
        Button submit = (Button) findViewById(R.id.submit_dates);
        submit.setOnClickListener(new CreateRequest());

        System.out.println("everything is working up past initialize submit button");

        // Set visibility of date range selections to invisible
        EditText startDateDisplay = (EditText) findViewById(R.id.start_date_display);
        startDateDisplay.setVisibility(View.INVISIBLE);
        EditText endDateDisplay = (EditText) findViewById(R.id.end_date_display);
        endDateDisplay.setVisibility(View.INVISIBLE);

        // Set the toggle settings for the "Specific dates" toggle
        setSpecificDatesToggle();

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.start_hours_selection, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the start and end hour spinners
        startHourSpinner = (Spinner) findViewById(R.id.start_hour);
        startHourSpinner.setAdapter(adapter);
        startHourSpinner.setOnItemSelectedListener(new startHourOnItemSelectedListener());
        endHourSpinner = (Spinner) findViewById(R.id.end_hour);

        // Set spinner values for start and end hour drop down menus
        initializeStartEndHour();

    }

    /**
     * Sets the visibility of the days of week according to the specificDatesToggle
     * Days of the week are visible when specificDatesToggle == false
     *
     */
    private void setSpecificDatesToggle() {
        toggle = (ToggleButton) findViewById(R.id.specific_dates);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent i = new Intent(getApplicationContext(), DateRangePicker.class);
                    startActivityForResult(i, DATE_RANGE_PICKER_REQUEST);
                    Button day = (Button) findViewById(R.id.sunday);
                    day.setVisibility(View.INVISIBLE);
                    day = (Button) findViewById(R.id.monday);
                    day.setVisibility(View.INVISIBLE);
                    day = (Button) findViewById(R.id.tuesday);
                    day.setVisibility(View.INVISIBLE);
                    day = (Button) findViewById(R.id.wednesday);
                    day.setVisibility(View.INVISIBLE);
                    day = (Button) findViewById(R.id.thursday);
                    day.setVisibility(View.INVISIBLE);
                    day = (Button) findViewById(R.id.friday);
                    day.setVisibility(View.INVISIBLE);
                    day = (Button) findViewById(R.id.saturday);
                    day.setVisibility(View.INVISIBLE);
                }
                else {
                    EditText startDateDisplay = (EditText) findViewById(R.id.start_date_display);
                    startDateDisplay.setVisibility(View.INVISIBLE);
                    EditText endDateDisplay = (EditText) findViewById(R.id.end_date_display);
                    endDateDisplay.setVisibility(View.INVISIBLE);
                    Button day = (Button) findViewById(R.id.sunday);
                    day.setVisibility(View.VISIBLE);
                    day = (Button) findViewById(R.id.monday);
                    day.setVisibility(View.VISIBLE);
                    day = (Button) findViewById(R.id.tuesday);
                    day.setVisibility(View.VISIBLE);
                    day = (Button) findViewById(R.id.wednesday);
                    day.setVisibility(View.VISIBLE);
                    day = (Button) findViewById(R.id.thursday);
                    day.setVisibility(View.VISIBLE);
                    day = (Button) findViewById(R.id.friday);
                    day.setVisibility(View.VISIBLE);
                    day = (Button) findViewById(R.id.saturday);
                    day.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Updates the display of the dropdown menu dynamically as user selects start time. Any end
     * times that occur after the selected start time will be grayed out and cannot be selected.
     */
    private void initializeStartEndHour() {
        String[] allHours = new String[]{
                "700",
                "800",
                "900",
                "1000",
                "1100",
                "1200",
                "1300",
                "1400",
                "1500",
                "1600",
                "1700",
                "1800",
                "1900",
                "2000",
                "2100",
                "2200",
                "2300",
                "2400"
        };

        List<String> allHoursList = new ArrayList<>(Arrays.asList(allHours));
        ArrayAdapter<String> adapterEndHour = new ArrayAdapter<String>(
                this, R.layout.support_simple_spinner_dropdown_item, allHoursList
        ) {
            @Override
            public boolean isEnabled(int position){
                if (startHourStr.equals("600")) {
                    return true;
                }
                if (startHourStr.equals("700") && position == 0) {
                    return false;
                }
                if (startHourStr.equals("800") && position < 2) {
                    return false;
                }
                if (startHourStr.equals("900") && position < 3) {
                    return false;
                }
                if (startHourStr.equals("1000") && position < 4) {
                    return false;
                }
                if (startHourStr.equals("1100") && position < 5) {
                    return false;
                }
                if (startHourStr.equals("1200") && position < 6) {
                    return false;
                }
                if (startHourStr.equals("1300") && position < 7) {
                    return false;
                }
                if (startHourStr.equals("1400") && position < 8) {
                    return false;
                }
                if (startHourStr.equals("1500") && position < 9) {
                    return false;
                }
                if (startHourStr.equals("1600") && position < 10) {
                    return false;
                }
                if (startHourStr.equals("1700") && position < 11) {
                    return false;
                }
                if (startHourStr.equals("1800") && position < 12) {
                    return false;
                }
                if (startHourStr.equals("1900") && position < 13) {
                    return false;
                }
                if (startHourStr.equals("2000") && position < 14) {
                    return false;
                }
                if (startHourStr.equals("2100") && position < 15) {
                    return false;
                }
                if (startHourStr.equals("2200") && position < 16) {
                    return false;
                }
                if (startHourStr.equals("2300") && position < 17) {
                    return false;
                }
                if (startHourStr.equals("2400")) {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(!isEnabled(position)) {
                    // Set the disable item text color
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        if (adapterEndHour == null) {
            System.out.println("adapter end hour is null.");
        }
        endHourSpinner.setAdapter(adapterEndHour);
        endHourSpinner.setOnItemSelectedListener(new endHourOnItemSelectedListener());
    }

    /**
     * A Listener that stores user selection of the start time
     */
    public class startHourOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            if (initialStartHourSelected) {
                Toast.makeText(parent.getContext(),
                        "Start Time : " + parent.getItemAtPosition(pos).toString(),
                        Toast.LENGTH_SHORT).show();
            }
            startHourStr = parent.getItemAtPosition(pos).toString();
            initialStartHourSelected = true;
        }
        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * A Listener that stores user selection of the end time
     */
    public class endHourOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            if (initialEndHourSelected) {
                Toast.makeText(parent.getContext(),
                        "End Time : " + parent.getItemAtPosition(pos).toString(),
                        Toast.LENGTH_SHORT).show();
            }
            endHourStr = parent.getItemAtPosition(pos).toString();
            initialEndHourSelected = true;
        }
        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Store the days of week that the user selected in the global variable "daysOfWeekChosen"
     */
    public void daysOfWeekSelected() {
        daysOfWeekChosen = new ArrayList<String>();
        ToggleButton day = (ToggleButton) findViewById(R.id.sunday);
        if (day.isChecked()) {
            daysOfWeekChosen.add("Sunday");
        }
        day = (ToggleButton) findViewById(R.id.monday);
        if (day.isChecked()) {
            daysOfWeekChosen.add("Monday");
        }
        day = (ToggleButton) findViewById(R.id.tuesday);
        if (day.isChecked()) {
            daysOfWeekChosen.add("Tuesday");
        }
        day = (ToggleButton) findViewById(R.id.wednesday);
        if (day.isChecked()) {
            daysOfWeekChosen.add("Wednesday");
        }
        day = (ToggleButton) findViewById(R.id.thursday);
        if (day.isChecked()) {
            daysOfWeekChosen.add("Thursday");
        }
        day = (ToggleButton) findViewById(R.id.friday);
        if (day.isChecked()) {
            daysOfWeekChosen.add("Friday");
        }
        day = (ToggleButton) findViewById(R.id.saturday);
        if (day.isChecked()) {
            daysOfWeekChosen.add("Saturday");
        }
    }

    /**
     * The form of the start and end dates from the "specific date" picker should
     * be in the format "MM/dd/yyyy" e.g. 02/23/2014.
     * for date string conversion, see: https://beginnersbook.com/2013/05/java-date-string-conversion/
     *
     * Retrieve the end and start dates from the DateRangePicker activity:
     * These dates will be returned by the intent from
     * the DateRangePicker activity
     *
      */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == DATE_RANGE_PICKER_REQUEST) {
            // Check if request was successful
            if (resultCode == RESULT_OK) {
                // Get end date and start date
                Bundle extras = data.getExtras();
                startDate = (String) extras.get("start_date");
                endDate = (String) extras.get("end_date");

                // Display start date and end date
                EditText startDateDisplay = (EditText) findViewById(R.id.start_date_display);
                startDateDisplay.setText("Start date: " + startDate);
                startDateDisplay.setVisibility(View.VISIBLE);
                EditText endDateDisplay = (EditText) findViewById(R.id.end_date_display);
                endDateDisplay.setText("End date: " + endDate);
                endDateDisplay.setVisibility(View.VISIBLE);
            }
            // Check if request was canceled
            if (resultCode == RESULT_CANCELED) {
                // Reset toggle to days of the week
                toggle.setChecked(false);
            }
        }
    }

    /**
     * Validate that all event dates are selected, and that they are selected properly.
     */
    private String[] validateEventDates(boolean specificDates) {
        daysOfWeekSelected();
        String[] dateRange;
        String[] errorArray = new String[0];
        if(specificDates) {
            if(startDate == null || endDate == null) {
                Toast.makeText(getApplicationContext(),
                        "Please select specific dates1", Toast.LENGTH_SHORT).show();
                return errorArray;
            }
            dateRange = new String[]{startDate, endDate};
        }
        else {
            if(daysOfWeekChosen == null){
                Toast.makeText(getApplicationContext(),
                        "Please select specific dates2", Toast.LENGTH_SHORT).show();
                return errorArray;
            }
            else {
                LinkedList<String> daysCast = new LinkedList<>();
                for (String day: daysOfWeekChosen) {
                    daysCast.add(dayToDate(day));
                }
                Object[] dateRangeObject = daysCast.toArray();
                dateRange = Arrays.copyOf(dateRangeObject, dateRangeObject.length, String[].class);
            }
        }
        if(endHourStr == null || startHourStr == null) {
            Toast.makeText(getApplicationContext(),
                    "Please select a valid expiration date", Toast.LENGTH_SHORT).show();
            return errorArray;
        }
        return dateRange;
    }


    /**
     * Creates an event via a post request, sending the event name, the dates/days of the week,
     * invited users, time range, and expiration date.
     */
    private class CreateRequest implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            final String eventName = ((EditText) findViewById(R.id.eventname)).getText().toString();

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
                        Intent goToLogin = new Intent(CreateEvent.this, SignInActivity.class);
                        startActivity(goToLogin);
                    }
                }.start();
            }

            // Check that user has selected days for the event
            boolean specificDates = ((ToggleButton) findViewById(R.id.specific_dates)).isChecked();
            final String specificDatesRequest = Boolean.toString(specificDates);
            final String[] dateRange = validateEventDates(specificDates);
            if (dateRange.length == 0) {
                return;
            }

            final String[] timeRange = new String[]{startHourStr, endHourStr};

            // Check if the expiration date entered is valid
            final String expirationDate = checkExpirationDate(specificDates);
            if (expirationDate == null) {
                return;
            }

            EditText invited = (EditText) findViewById(R.id.usernames);
            String users = invited.getText().toString();
            final String[] invitedUsers = users.split(" ");

            // Create a createEvent request, adding the event to the database
            final String url = "http://10.0.3.2:3000/createEvent";
            StringRequest createEventRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject responseObject = new JSONObject(response);
                                String error = responseObject.getString("error");
                                if (!error.equals("null")) {
                                    Toast.makeText(getApplicationContext(), error,
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    String eventName = responseObject.getString("eventName");
                                    final int eventIdentifier = responseObject.getInt("eventId");
                                    Toast.makeText(getApplicationContext(), "Created event " + eventName,
                                            Toast.LENGTH_SHORT).show();
                                    new CountDownTimer(1500, 1500) {
                                        @Override
                                        public void onTick(long l) {

                                        }

                                        @Override
                                        public void onFinish() {
                                            Intent toEvent = new Intent(CreateEvent.this, EventPage.class);
                                            toEvent.putExtra("eventID", eventIdentifier);
                                            startActivity(toEvent);
                                        }
                                    }.start();
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
                    params.put("eventName", eventName);
                    params.put("jwt", token);
                    params.put("specificDates", specificDatesRequest);
                    params.put("dateRange", Arrays.toString(dateRange));
                    params.put("invitedUsers", Arrays.toString(invitedUsers));
                    params.put("timeRange", Arrays.toString(timeRange));
                    if (!expirationDate.equals("")) {
                        params.put("expirationDate", expirationDate);
                    }
                    return params;

                }
            };
            queue.add(createEventRequest);
        }

       /**
        * Validates a user's expiration date input. Does not permit expiration dates that have
        * already passed or that are within the interval of selected dates for the event
        *
        * @param specificDates
        * @return expiryTest (result of expiration date test)
        */
       private String checkExpirationDate(boolean specificDates) {
           EditText expiryDateField = findViewById(R.id.expiryDate);
           String expiryTest = expiryDateField.getText().toString();
           if (!expiryTest.isEmpty() && specificDates) {
               if (!expiryTest.contains("/")) {
                   Toast.makeText(getApplicationContext(), "Please select a valid expiration date", Toast.LENGTH_SHORT).show();
                   return null;
               }
               String[] expirySplit = expiryTest.split("/");
               if (expirySplit.length != 3) {
                   Toast.makeText(getApplicationContext(), "Please select a valid expiration date", Toast.LENGTH_SHORT).show();
                   return null;
               }
               int expiryMonth = Integer.parseInt(expirySplit[0]);
               int expiryDay = Integer.parseInt(expirySplit[1]);
               int expiryYear = Integer.parseInt(expirySplit[2]);
               String[] startDateSplit = startDate.split("/");
               int startMonth = Integer.parseInt(startDateSplit[0]);
               int startDay = Integer.parseInt(startDateSplit[1]);
               int startYear = Integer.parseInt(startDateSplit[2]);
               if (startYear > expiryYear || (startYear == expiryYear && startMonth > expiryMonth)
                       || (startYear == expiryYear && startMonth == expiryMonth && startDay > expiryDay)) {
                   Toast.makeText(getApplicationContext(), "Please select a valid expiration date", Toast.LENGTH_SHORT).show();
                   return null;
               }
               String[] endDateSplit = endDate.split("/");
               int endMonth = Integer.parseInt(endDateSplit[0]);
               int endDay = Integer.parseInt(endDateSplit[1]);
               int endYear = Integer.parseInt(endDateSplit[2]);
               if (endYear > expiryYear || (endYear == expiryYear && endMonth > expiryMonth)
                       || (endYear == expiryYear && endMonth == expiryMonth && endDay > expiryDay)) {
                   Toast.makeText(getApplicationContext(), "Please select a valid expiration date", Toast.LENGTH_SHORT).show();
                   return null;
               }
           }
           return expiryTest;
       }
   }
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
