package com.example.learnleaf;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    Button back;
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

        back.setOnClickListener(v ->
        {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
        });

        submit.setOnClickListener(v -> {
            if (email == null) {
                Toast.makeText(Login.this, "Error: Fields not initialized", Toast.LENGTH_SHORT).show();
                return;  // Exit the listener if fields are not initialized
            }

            String em = email.getText().toString().trim();
            String pw = password.getText().toString().trim();

            if (em.isEmpty() || pw.isEmpty()) {
                Toast.makeText(Login.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Use the Firebase class to perform login
                firebase.signIn(em, pw, new Firebase.AuthCallback() {
                    @Override
                    public void onSuccess(FirebaseUser user) {
                        Toast.makeText(Login.this, "Authentication successful", Toast.LENGTH_SHORT).show();
                        // Proceed to next activity
                        Intent intent = new Intent(Login.this, Home.class);
                        startActivity(intent);
                        finish(); // Optional: close the Login activity
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