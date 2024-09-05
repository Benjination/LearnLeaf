package com.example.learnleaf;
import com.example.learnleaf.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button login;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        login = findViewById(R.id.Login);

        login.setOnClickListener(v ->
        {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        });
    }
}