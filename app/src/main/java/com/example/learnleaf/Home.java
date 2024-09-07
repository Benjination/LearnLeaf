package com.example.learnleaf;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        Button main;

        main = findViewById(R.id.main);

        main.setOnClickListener(v ->
        {
            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
        });
    }
}