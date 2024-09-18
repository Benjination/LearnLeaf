package com.example.learnleaf;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    Button login;
    Button signup;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        login = findViewById(R.id.login);
        signup = findViewById(R.id.signup);

        login.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        });

        signup.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUp.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // User is signed in, navigate to Home page
            navigateToHome();
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, Home.class);
        startActivity(intent);
        finish(); // This closes the MainActivity so the user can't go back to it using the back button
    }
}