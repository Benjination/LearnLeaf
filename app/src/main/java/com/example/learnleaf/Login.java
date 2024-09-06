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

    String PW, EM;

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
            Toast.makeText(this, "Submitted", Toast.LENGTH_SHORT).show();
        });

    }
}