package com.example.learnleaf;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

import java.util.HashMap;
import java.util.Map;

//I am HACKERMAN
public class SignUp extends AppCompatActivity {

    String email, password, confirm, name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);
        FirebaseApp.initializeApp(this);

        Button back = findViewById(R.id.back);;
        Button submit = findViewById(R.id.submit);
        EditText EM = findViewById(R.id.email);
        EditText PW = findViewById(R.id.password);
        EditText con = findViewById(R.id.confirm);
        EditText NM = findViewById(R.id.username);

        back.setOnClickListener(v ->
        {
            Intent intent = new Intent(SignUp.this, MainActivity.class);
            startActivity(intent);
        });

        submit.setOnClickListener(v ->
        {
            //Checks if EditTexts are initialized
            if (EM == null || PW == null) {
                Toast.makeText(SignUp.this, "Error: Fields not initialized", Toast.LENGTH_SHORT).show();
                return;  // Exit the listener if fields are not initialized
            }
            //Captures email, password, and confirm
            email = EM.getText().toString();
            password = PW.getText().toString();
            confirm = con.getText().toString();
            name = NM.getText().toString();

            //Tests if email and password are empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUp.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {

                //Checks validity of user input
                if (!isValidEmail(email)) {
                    Toast.makeText(SignUp.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    return;
                }
                //Checks password matches confirm
                if(!password.equals(confirm))
                {
                    Toast.makeText(SignUp.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidPassword(password)) {
                    Toast.makeText(SignUp.this, "Password must include letters, numbers,\n and a special symbol. " +
                            "It must also be at least 6 characters long", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Adds user to Database and Firestore
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                String userId = firebaseUser.getUid();

                                // Create a Map of user data
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("dateFormat", "MM/dd/yyyy"); // Default date format
                                userData.put("email", email);
                                userData.put("name", name); // Assuming you have a 'name' variable
                                userData.put("notifications", true); // Default to true
                                userData.put("notificationFrequency", 2); // Default frequency
                                userData.put("timeFormat", "HH:mm"); // Default time format

                                // Add user data to Firestore
                                db.collection("users").document(userId)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Firestore", "User data successfully written!");
                                            Toast.makeText(SignUp.this, "User created successfully", Toast.LENGTH_SHORT).show();

                                            // Navigate to Login activity
                                            Intent intent = new Intent(SignUp.this, Login.class);
                                            startActivity(intent);
                                            finish(); // Optional: close the SignUp activity
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w("Firestore", "Error writing user data", e);
                                            Toast.makeText(SignUp.this, "Failed to save user data: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });

                            } else {
                                Log.w("FirebaseAuth", "createUserWithEmail:failure", task.getException());
                                Toast.makeText(SignUp.this, "Authentication failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
}

    boolean isValidPassword(String password) {
        // Check if password is at least 6 characters long
        if (password.length() < 6) {
            return false;
        }
        //Check for alpha
        boolean hasAlpha = password.matches(".*[a-zA-Z].*");
        //Check for Numeric
        boolean hasNumeric = password.matches(".*\\d.*");
        //Check for Special
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        return hasAlpha && hasNumeric && hasSpecial;
    }
    boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        //Android's built-in Patterns class for email validation
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public class User {
        public String name;
        public String email;

        public User() {
            // Default constructor required for Firebase
        }

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}