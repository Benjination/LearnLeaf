package learn.leaf.learnleaf;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.credentials.CredentialManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {

    private static final String TAG = "Login";

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private ImageButton googleBtn;
    private Button back, submit;
    private EditText email, password;
    private TextView forgot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Initialize Firebase Auth and Credential Manager
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // Initialize views and listeners
        initializeViews();
        setListeners();
    }

    private void initializeViews() {
        back = findViewById(R.id.back);
        submit = findViewById(R.id.submit);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        forgot = findViewById(R.id.fgPassword);
    }

    private void setListeners() {
        back.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, SignUp.class));
            finish();
        });
        submit.setOnClickListener(v -> signInWithEmail());
        forgot.setOnClickListener(v -> forgotPassword());
    }

    private void forgotPassword() {
        String em = email.getText().toString().trim();
        if(isValidEmail(em))
        {
            FirebaseAuth.getInstance().sendPasswordResetEmail(em)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Password reset email sent successfully
                                Toast.makeText(Login.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                            } else {
                                // Failed to send password reset email
                                Toast.makeText(Login.this, "Failed to send password reset email", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
        else
        {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void signInWithEmail() {
        String em = email.getText().toString().trim();
        String pw = password.getText().toString().trim();

        if (em.isEmpty() || pw.isEmpty()) {
            Toast.makeText(Login.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(em, pw)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(Login.this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Login.this, Tasks.class));
            finish();
        }
    }
}
