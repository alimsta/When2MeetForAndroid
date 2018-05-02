package com.spring2018.cis350.group7.when2meetmobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;
import android.os.Handler;

/**
 * Reset password activity that allows a user to reset his/her password
 *
 */
public class ResetPassword extends AppCompatActivity {

    private RequestQueue queue;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        handler = new Handler();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        setTitle("Reset Password");
        queue = Volley.newRequestQueue(this);
    }

    // Reset password button-click listener; tells user whether password change was successful or not
    public void onClick(View view) {
        final TextView pw1 = findViewById(R.id.passwordText);
        final String pwString1 = pw1.getText().toString();
        TextView pw2 = findViewById(R.id.confirmText);
        String pwString2 = pw2.getText().toString();

        // If both input passwords match, then send a post request to update the password.
        if (pwString1.equals(pwString2)) {
            final String url = "http://10.0.3.2:3000/newPassword";
            StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // If the password change was successful, display a success Toast
                            if (response.equals("true")) {
                                Toast.makeText(getApplicationContext(),
                                        "Password Changed!", Toast.LENGTH_LONG).show();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                }, 1250);
                            }
                            // If the password change was unsuccessful, display a failure Toast
                            else {
                                Toast.makeText(getApplicationContext(),
                                        "Unable to change password.", Toast.LENGTH_LONG).show();
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
                    SharedPreferences appPrefernces = getSharedPreferences("when2meetPreferences", 0);
                    final String token = appPrefernces.getString("token", "");
                    Map<String, String>  params = new HashMap<String, String>();
                    params.put("jwt", token);
                    params.put("password", pwString1);
                    return params;
                }
            };
            queue.add(postRequest);
        }
        else {
            pw1.setText("");
            pw2.setText("");
            Toast.makeText(this, "Please enter matching passwords.", Toast.LENGTH_LONG).show();
        }
    }
}
