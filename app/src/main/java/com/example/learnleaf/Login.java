package com.example.learnleaf;
import com.example.learnleaf.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Login extends AppCompatActivity {

    Button back;
    Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        back = findViewById(R.id.back);
        submit = findViewById(R.id.submit);
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);

        back.setOnClickListener(v ->
        {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
        });

        submit.setOnClickListener(v -> {
            if (email == null || password == null) {
                Toast.makeText(Login.this, "Error: Fields not initialized", Toast.LENGTH_SHORT).show();
                return;  // Exit the listener if fields are not initialized
            }

            String em = email.getText().toString();
            String pw = password.getText().toString();

            if (em.isEmpty() || pw.isEmpty()) {
                Toast.makeText(Login.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Login.this, "Submitted", Toast.LENGTH_SHORT).show();
                System.out.println("Email: " + em);
                System.out.println("Password: " + pw);

                //  perform login validation
                // If login is successful,
                // Else handle error
                Intent intent = new Intent(Login.this, Home.class);
                startActivity(intent);
            }
        });

    }
}