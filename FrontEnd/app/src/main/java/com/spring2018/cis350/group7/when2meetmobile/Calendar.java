package com.spring2018.cis350.group7.when2meetmobile;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;

/**
 * Initializes the layout and on click activity of the calendar used for date-range picking in
 * event creation.
 * @method onCreate - initialize the table layout. Set the calendar color to green for
 * a date that has been clicked.
 */
public class Calendar extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        TableLayout layout = new TableLayout (this);
        layout.setLayoutParams( new TableLayout.LayoutParams(4,5) );

        layout.setPadding(0,0,0,0);

        int rows = 6;  //Would be dynamically changed based on times selected
        int cols = 3;  //Would be dynamically changed based on days selected
        for (int f=0; f<= rows; f++) {
            TableRow tr = new TableRow(this);
            for (int c = 0; c <= cols; c++) {
                Button b = new Button(this);
                b.setBackgroundColor(Color.TRANSPARENT);
                b.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View view) {
                                             view.setBackgroundColor(Color.GREEN);
                                         }
                                     }
                );
                tr.addView(b, 30, 30);
            }
            layout.addView(tr);
        }

        super.setContentView(layout);
    }

}
