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

public class SignUp extends AppCompatActivity {

    String email, password, confirm;

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

                //Adds user to Database
                FirebaseAuth mAuth = FirebaseAuth.getInstance();

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                Log.d("FirebaseAuth", "createUserWithEmail:success");
                                Toast.makeText(SignUp.this, "User created successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SignUp.this, Login.class);
                                startActivity(intent);
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
}