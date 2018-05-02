package com.spring2018.cis350.group7.when2meetmobile;
/**
 * LaunchScreen is the first screen that is displayed when a user is not persistently logged in.
 * It allows a user to sign in to an existing account or sign up for a new one.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class LaunchScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_screen);

        // Check if user has already logged in (and did not log out)
        SharedPreferences sp = getSharedPreferences("login", Context.MODE_PRIVATE);
        if (sp.getBoolean("logged", false)) {
            // If user had already logged in, immediately redirect to Main Menu
            Intent i = new Intent(this, MainMenu.class);
            startActivity(i);
        }
    }

    // Start SignUp activity when "Sign up" button is clicked
    public void onSignUpClick(View view) {
        Intent i = new Intent(this, SignUp.class);
        startActivity(i);
    }

    // Start SignInActivity when "Sign in" button is clicked
    public void onSignInClick(View view) {
        Intent i = new Intent(this, SignInActivity.class);
        startActivity(i);
    }
}
