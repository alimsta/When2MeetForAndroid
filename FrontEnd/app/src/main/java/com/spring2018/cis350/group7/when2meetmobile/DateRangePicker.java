package com.spring2018.cis350.group7.when2meetmobile;
/**
 * DateRangePicker allows a user to pick a range of dates from a calendar when they create an event.
 */

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.savvi.rangedatepicker.CalendarPickerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DateRangePicker extends AppCompatActivity {
    CalendarPickerView calendar;
    List<Date> selectedDates;
    boolean datesWereSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_range_picker);

        // Create date range calendar
        TimeZone timeZone = TimeZone.getDefault();
        Locale locale = Locale.getDefault();
        calendar = (CalendarPickerView) findViewById(R.id.calendar_view);
        Calendar nextYear = Calendar.getInstance(timeZone, locale);
        nextYear.add(Calendar.YEAR, 1);
        Calendar thisYear = Calendar.getInstance(timeZone, locale);
        Calendar lastYear = Calendar.getInstance(timeZone, locale);
        lastYear.add(Calendar.YEAR, -1);
        calendar.init(thisYear.getTime(), nextYear.getTime()) //
                .inMode(CalendarPickerView.SelectionMode.RANGE) //
                .withSelectedDate(new Date());

        // DO NOT DELETE THIS. list is required for calendar initialization
        ArrayList<Integer> list = new ArrayList<>();
        calendar.deactivateDates(list);
    }

    // Called when user clicks button "OK" or "CANCEL"
    public void onClick(View view) {
        Button selectedButton = (Button) findViewById(view.getId());
        // "OK" button
        if (selectedButton.getText().equals("OK")) {
            selectedDates = calendar.getSelectedDates();
            // Throw an error message if user has not selected any dates
            if (selectedDates.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please select a date range!", Toast.LENGTH_LONG).show();
            }
            // If user has selected dates, stage the dates to be passed by the Intent
            else {
                datesWereSelected = true;
                finish();
            }
        }
        // "CANCEL" button
        if (selectedButton.getText().equals("Cancel")) {
            finish();
        }
    }

    // Convert a Date object to a String (so it can be passed by the Intent) - dd/MM/yyyy
    public String convertDateToString(Date date) {
        String dateString = null;
        SimpleDateFormat sdfr = new SimpleDateFormat("MM/dd/yyyy");
        try {
            dateString = sdfr.format( date );
        }
        catch (Exception ex ){
            System.out.println(ex);
        }
        return dateString;
    }

    @Override
    public void finish() {
        // Return with no dates if "CANCEL" button
        Intent i = new Intent();
        if (!datesWereSelected) {
            setResult(RESULT_CANCELED, i);
            super.finish();
        }

        // Prepare intent with both start and end date if "OK" button
        else {
            Date startDate = selectedDates.get(0);
            Date endDate = selectedDates.get(selectedDates.size() - 1);
            String startDateStr = convertDateToString(startDate);
            String endDateStr = convertDateToString(endDate);
            i.putExtra("start_date", startDateStr);
            i.putExtra("end_date", endDateStr);
            setResult(RESULT_OK, i);
            super.finish();
        }
    }
}
