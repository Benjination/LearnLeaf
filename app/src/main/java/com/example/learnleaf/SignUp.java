package com.example.learnleaf;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUp extends AppCompatActivity {
    private static final String TAG = "SignUp";

    private String email;
    private String password;
    private String name;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText EM, PW, con, NM;
    private Button submit;
    private TextView Login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.sign_up);
            initializeViews();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SignUp activity", e);
            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }



        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(SignUp.this, Tasks.class));
            finish();
            return;
        }

        setupListeners();
    }

    private void initializeViews() {
        submit = findViewById(R.id.submit);
        EM = findViewById(R.id.email);
        PW = findViewById(R.id.password);
        if (PW != null) {
            PW.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        con = findViewById(R.id.confirm);
        NM = findViewById(R.id.username);
        Login = findViewById(R.id.login);
    }

    private void setupListeners() {
        Login.setOnClickListener(v -> startActivity(new Intent(SignUp.this, Login.class)));

        submit.setOnClickListener(v -> {
            if (!validateInputs()) return;
            createUser();
        });
    }

    private boolean validateInputs() {
        email = EM.getText().toString().trim();
        password = PW.getText().toString();
        String confirm = con.getText().toString();
        name = NM.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must include letters, numbers, and a special symbol. It must also be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void createUser() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUserProfile(Objects.requireNonNull(user));
                        saveAdditionalUserInfo(user);
                        Toast.makeText(SignUp.this, "User created successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUp.this, Login.class));
                        finish();
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUp.this, "Authentication failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                    }
                });
    }

    private void saveAdditionalUserInfo(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("dateFormat", "MM/dd/yyyy");
        userData.put("email", email);
        userData.put("name", name);
        userData.put("notifications", true);
        userData.put("notificationFrequency", 2);
        userData.put("timeFormat", "HH:mm");

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved successfully"))
                .addOnFailureListener(e -> Log.w(TAG, "Error saving user data", e));
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                password.matches(".*[a-zA-Z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }


}