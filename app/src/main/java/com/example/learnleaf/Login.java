package com.example.learnleaf;
import com.example.learnleaf.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Login extends AppCompatActivity {


    Button back;
    Button submit;

    TextView email;
    TextView password;

    String PW = "Blank", EM = "Also Blank";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        back = findViewById(R.id.back);
        submit = findViewById(R.id.submit);

        back.setOnClickListener(v ->
        {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
        });
        submit.setOnClickListener(v ->
        {
            EM = email.getText().toString();
            PW = password.getText().toString();
            if (EM.isEmpty() || PW.isEmpty()) {
                Toast.makeText(Login.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
            if (!EM.isEmpty() && !PW.isEmpty()){
                Toast.makeText(Login.this, "Submitted", Toast.LENGTH_SHORT).show();
                System.out.println("Email:" + EM);
                System.out.println("Password:" + PW);
            }
            if (email == null || password == null) {
                Toast.makeText(Login.this, "Error: Fields not initialized", Toast.LENGTH_SHORT).show();
                return;  // Exit the listener if fields are not initialized
            }
            Intent intent = new Intent(Login.this, Login.class);
            startActivity(intent);
        });

    }
}