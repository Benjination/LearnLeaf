package com.example.learnleaf;
import com.example.learnleaf.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Login extends AppCompatActivity {

    Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        back = findViewById(R.id.back);

        back.setOnClickListener(v ->
        {
            Intent intent = new Intent(Login.this, MainActivity.class);
            startActivity(intent);
        });
    }
}