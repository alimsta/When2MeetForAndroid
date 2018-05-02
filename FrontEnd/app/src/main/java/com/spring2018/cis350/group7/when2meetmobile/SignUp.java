package com.spring2018.cis350.group7.when2meetmobile;
/**
 * SignUp is the activity that allows users to create a new account by entering a username, password,
 * and name.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUp extends AppCompatActivity {

    private static int RESULT_LOAD_IMAGE = 1;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ImageView errorView = findViewById(R.id.error);
        errorView.setVisibility(View.INVISIBLE);
        ImageView checkView = findViewById(R.id.success);
        checkView.setVisibility(View.INVISIBLE);
        EditText usernameEditText = findViewById(R.id.usernameInput);
        usernameEditText.addTextChangedListener(usernameWatcher);
        EditText passwordEditText = findViewById(R.id.passwordInput);
        passwordEditText.addTextChangedListener(passwordWatcher);
        EditText nameEditText = findViewById(R.id.nameInput);
        nameEditText.addTextChangedListener(nameWatcher);
        queue = Volley.newRequestQueue(this);
        setTitle("Sign-Up Here!");
    }

    private final TextWatcher usernameWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            EditText usernameEditText = findViewById(R.id.usernameInput);
            String input = usernameEditText.getText().toString();
            isAvailableUsername(input);
        }

        public void afterTextChanged(Editable s) {
        }
    };

    private final TextWatcher passwordWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            ImageView check = findViewById(R.id.success);
            if (check.getVisibility() == View.VISIBLE && !reqFieldsCheck()) {
                Button button = findViewById(R.id.signUpButton);
                button.setEnabled(true);
            } else {
                Button button = findViewById(R.id.signUpButton);
                button.setEnabled(false);
            }
        }

        public void afterTextChanged(Editable s) {
        }
    };

    private final TextWatcher nameWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            ImageView check = findViewById(R.id.success);
            if (check.getVisibility() == View.VISIBLE && !reqFieldsCheck()) {
                Button button = findViewById(R.id.signUpButton);
                button.setEnabled(true);
            } else {
                Button button = findViewById(R.id.signUpButton);
                button.setEnabled(false);
            }
        }

        public void afterTextChanged(Editable s) {
        }
    };

    // Allow user to select a profile picture
    public void picClick(View arg0) {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    // Check if required fields are filled out
    public boolean reqFieldsCheck() {
        boolean isEmpty = false;
        EditText fullNameView = findViewById(R.id.nameInput);
        EditText usernameView = findViewById(R.id.usernameInput);
        EditText passwordView = findViewById(R.id.passwordInput);
        fullNameView.setError(null);
        usernameView.setError(null);
        passwordView.setError(null);
        String nameInput = fullNameView.getText().toString();
        String usernameInput = usernameView.getText().toString();
        String passwordInput = passwordView.getText().toString();
        if (TextUtils.isEmpty(nameInput)) {
            fullNameView.setError("This field is required!");
            //fullNameView.requestFocus();
            isEmpty = true;
        }
        if (TextUtils.isEmpty(usernameInput)) {
            usernameView.setError("This field is required!");
            //usernameView.requestFocus();
            isEmpty = true;
        }
        if (TextUtils.isEmpty(passwordInput)) {
            passwordView.setError("This field is required!");
            //passwordView.requestFocus();
            isEmpty = true;
        }
        return isEmpty;
    }

    // Check for valid inputs when user clicks "Submit"
    public void submitClick (View view) {
        EditText fullNameView = findViewById(R.id.nameInput);
        EditText usernameView = findViewById(R.id.usernameInput);
        EditText passwordView = findViewById(R.id.passwordInput);
        String fname = fullNameView.getText().toString();
        String uname = usernameView.getText().toString();
        String password = passwordView.getText().toString();
        postNewAccount(fname, uname, password);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
        }
    }

    private void isAvailableUsername(String usernameInput) {
        final String input = usernameInput;
        StringRequest postRequest = new StringRequest(Request.Method.POST, "http://10.0.3.2:3000/newUsername",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("false") && !input.equals("")) {
                            // hide the error, display the check mark
                            ImageView errorView = findViewById(R.id.error);
                            errorView.setVisibility(View.INVISIBLE);
                            ImageView checkView = findViewById(R.id.success);
                            checkView.setVisibility(View.VISIBLE);

                            // turn the username input box green
                            EditText username = findViewById(R.id.usernameInput);

                            // Create a border programmatically
                            ShapeDrawable shape = new ShapeDrawable(new RectShape());
                            shape.getPaint().setColor(Color.GREEN);
                            shape.getPaint().setStyle(Paint.Style.STROKE);
                            shape.getPaint().setStrokeWidth(3);

                            // Assign the created border to EditText widget
                            username.setBackground(shape);

                            // if the required fields are not empty, enable sign up button
                            // if required fields are empty, disable sign up button
                            if (!reqFieldsCheck()) {
                                Button signupButton = findViewById(R.id.signUpButton);
                                signupButton.setEnabled(true);
                            } else {
                                Button signupButton = findViewById(R.id.signUpButton);
                                signupButton.setEnabled(false);
                            }
                        } else {
                            // hide the check mark, display the error
                            ImageView checkView = findViewById(R.id.success);
                            checkView.setVisibility(View.INVISIBLE);
                            ImageView errorView = findViewById(R.id.error);
                            errorView.setVisibility(View.VISIBLE);

                            // turn the username input box red
                            EditText username = findViewById(R.id.usernameInput);

                            // Create a border programmatically
                            ShapeDrawable shape = new ShapeDrawable(new RectShape());
                            shape.getPaint().setColor(Color.RED);
                            shape.getPaint().setStyle(Paint.Style.STROKE);
                            shape.getPaint().setStrokeWidth(3);

                            // Assign the created border to EditText widget
                            username.setBackground(shape);

                            // disable sign up button
                            Button signupButton = findViewById(R.id.signUpButton);
                            signupButton.setEnabled(false);
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
                params.put("text", input);
                return params;
            }
        };
        queue.add(postRequest);
    }

    public void postNewAccount(String fullName, String username, String password) {
        final String fname = fullName;
        final String uname = username;
        final String pass = password;
        StringRequest postRequest = new StringRequest(Request.Method.POST, "http://10.0.3.2:3000/newAccount",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // if response is success, switch the intent
                        try {
                            JSONObject responseObj = new JSONObject(response);
                            final String token = responseObj.getString("token");
                            Toast.makeText(getApplicationContext(), "Your account has been created!", Toast.LENGTH_LONG).show();
                            new CountDownTimer(1500, 1500) {

                                public void onTick(long millisUntilFinished) {}

                                public void onFinish() {
                                    sendTokenRedirect(token);
                                }
                            }.start();
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Error: " +
                                    e.getMessage(), Toast.LENGTH_LONG).show();
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

                params.put("fname", fname);
                params.put("uname", uname);
                params.put("pass", pass);
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
        Intent i = new Intent(SignUp.this, MainMenu.class);
        startActivity(i);
    }
    // TODO - consider having a "confirm password" text box

}
