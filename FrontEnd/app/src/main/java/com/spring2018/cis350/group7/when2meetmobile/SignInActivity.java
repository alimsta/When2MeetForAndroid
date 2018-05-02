package com.spring2018.cis350.group7.when2meetmobile;
/**
 * SignInActivity is the activity that allows users to sign in by entering a username and password.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {
    RequestQueue queue;
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        TextView errorMsg = findViewById(R.id.errorMessage);
        errorMsg.setVisibility(View.INVISIBLE);
        queue = Volley.newRequestQueue(this);
        setTitle("Sign In Here!");
    }

    public void onSigninClick(View view) {
        EditText usernameInput = findViewById(R.id.username);
        EditText passwordInput = findViewById(R.id.password);
        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            TextView errorMsg = findViewById(R.id.errorMessage);
            errorMsg.setTextColor(Color.RED);
            errorMsg.setText("Please fill in both fields.");
            errorMsg.setVisibility(View.VISIBLE);
        } else {
            TextView errorMsg = findViewById(R.id.errorMessage);
            errorMsg.setVisibility(View.INVISIBLE);
            isValidSignup(username, password);
        }
    }

    private void isValidSignup(String usernameInput, String passwordInput) {
        final String unameInput = usernameInput;
        final String passInput = passwordInput;
        StringRequest postRequest = new StringRequest(Request.Method.POST, "http://10.0.3.2:3000/signin",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        if (response.equals("no user found")) {
                            TextView errorMsg = findViewById(R.id.errorMessage);
                            errorMsg.setTextColor(Color.RED);
                            errorMsg.setText("Invalid username and password combo. Please try again!");
                            errorMsg.setVisibility(View.VISIBLE);

                        } else {
                            TextView errorMsg = findViewById(R.id.errorMessage);
                            errorMsg.setVisibility(View.INVISIBLE);
                            try {
                                JSONObject responseObj = new JSONObject(response);
                                final String token = responseObj.getString("token");
                                storeUserLogin();
                                sendTokenRedirect(token);
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "Error: " +
                                        e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("Server error");
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("uname", unameInput);
                params.put("pass", passInput);
                return params;
            }
        };
        queue.add(postRequest);
    }

    public void sendTokenRedirect(String token) {
        SharedPreferences appPref = getSharedPreferences("when2meetPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = appPref.edit();
        editor.putString("token", token);
        editor.commit();
        Intent i = new Intent(SignInActivity.this, MainMenu.class);
        startActivity(i);
    }

    private void storeUserLogin() {
        Log.v("LOGGED USER LOGIN", "success");
        SharedPreferences sp = getSharedPreferences("login", Context.MODE_PRIVATE);
        sp.edit().putBoolean("logged",true).apply();
    }
}
