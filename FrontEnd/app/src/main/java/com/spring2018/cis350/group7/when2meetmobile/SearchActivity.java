package com.spring2018.cis350.group7.when2meetmobile;

/**
 * SearchActivity handles all logic for the search function.
 * Allows the current user to search if an account associated with a
 * specific username exists.
*/

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        TextView resultView = findViewById(R.id.searchResult);
        resultView.setVisibility(View.INVISIBLE);
        queue = Volley.newRequestQueue(this);
    }

    // event handler for search click, searches if a username exists
    public void onSearchClick(View view) {
        EditText searchEdit = findViewById(R.id.searchBar);
        final String searchTerm = searchEdit.getText().toString();
        final String url = "http://10.0.3.2:3000/newUsername";
        StringRequest makeSearchRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("true")) {
                            // display username
                            TextView resultView = findViewById(R.id.searchResult);
                            resultView.setText(searchTerm + " has an account.");
                            resultView.setVisibility(View.VISIBLE);
                        } else {
                            // display "no user found"
                            TextView resultView = findViewById(R.id.searchResult);
                            resultView.setText(searchTerm + " does not have an account.");
                            resultView.setVisibility(View.VISIBLE);
                        }
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
                SharedPreferences appPreferences = getSharedPreferences("when2meetPreferences", 0);
                final String token = appPreferences.getString("token", "");
                params.put("text", searchTerm);
                params.put("jwt", token);
                return params;

            }
        };
        queue.add(makeSearchRequest);
    }
}
