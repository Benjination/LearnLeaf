package com.example.learnleaf;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class SignUp extends AppCompatActivity {

    Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);
        back = findViewById(R.id.back);

        back.setOnClickListener(v ->
        {
            Intent intent = new Intent(SignUp.this, MainActivity.class);
            startActivity(intent);
        });
    }

}