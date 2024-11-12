package com.example.learnleaf;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseUser;

import kotlinx.coroutines.scheduling.Task;

public class Login extends AppCompatActivity {

    TextView back;
    Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        Firebase firebase = Firebase.getInstance(this);

        back = findViewById(R.id.back);
        submit = findViewById(R.id.submit);
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        //back button controls-> navigate from login to SignUp
        //Actual text says "Sign Up", not "Back"
        back.setOnClickListener(v ->
        {
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
            finish();
        });

        //submit button controls-> tests user input is valid finds user information on database and allows access to
        //User-Authentication restricted pages
        submit.setOnClickListener(v -> {
            if (email == null) {
                Toast.makeText(Login.this, "Error: Fields not initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            String em = email.getText().toString().trim();
            String pw = password.getText().toString().trim();

            //Checks Empty Fields
            if (em.isEmpty() || pw.isEmpty()) {
                Toast.makeText(Login.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                //Attempts to find user in Firestore, Authenticates, navigates to Tasks on Success
                firebase.signIn(em, pw, new Firebase.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        Toast.makeText(Login.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Login.this, Tasks.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Toast.makeText(Login.this, "Authentication failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

    }
}