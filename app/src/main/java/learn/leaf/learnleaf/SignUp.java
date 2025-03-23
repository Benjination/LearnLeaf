package learn.leaf.learnleaf;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import nu.aaro.gustav.passwordstrengthmeter.PasswordStrengthMeter;

public class SignUp extends AppCompatActivity {
    private static final String TAG = "SignUp";
    private String email;
    private String password;
    private String name;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText EM, PW, con, NM;
    private Button submit;
    private TextView Login;
    private PasswordStrengthMeter passwordStrengthMeter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.sign_up);
            initializeViews();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SignUp activity", e);
            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Asterick Replacement for user input
        PW.setTransformationMethod(new AsteriskPasswordTransformationMethod());
        con.setTransformationMethod(new AsteriskPasswordTransformationMethod());


        //Opens connection to database
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //If user is already signed in, bypass everything and go to tasks
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(SignUp.this, Tasks.class));
            finish();
            return;
        }
        setupListeners();
    }

    //Called in onCreate as part of the display process
    private void initializeViews() {
        submit = findViewById(R.id.submit);
        EM = findViewById(R.id.email);
        PW = findViewById(R.id.password);
        if (PW != null) {
            PW.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        con = findViewById(R.id.confirm);
        NM = findViewById(R.id.username);
        Login = findViewById(R.id.login);
        passwordStrengthMeter = findViewById(R.id.passwordStrengthMeter);
        passwordStrengthMeter.setEditText(PW);
    }


    private void setupListeners() {
        //Option if user already has an account and wants to jump to Log In page
        Login.setOnClickListener(v -> startActivity(new Intent(SignUp.this, Login.class)));

        //Checks input validation and creates new user
        submit.setOnClickListener(v -> {
            if (!validateInputs()) return;
            createUser();
        });
    }

    //Several checks for email and password to ensure valid input
    private boolean validateInputs() {
        email = EM.getText().toString().trim();
        password = PW.getText().toString();
        String confirm = con.getText().toString();
        name = NM.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        int passwordStrength = calculatePasswordStrength(password);
        if (passwordStrength < 2) {
            Toast.makeText(this, "Password is too weak. It must include letters, numbers, and a special symbol, and be at least 6 characters long", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private int calculatePasswordStrength(String password) {
        if (password.length() < 6) {
            return 0;
        } else if (password.matches("^[a-zA-Z]+$")) {
            return 1;
        } else if (password.matches("^[a-zA-Z0-9]+$")) {
            return 2;
        } else if (password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$")) {
            return 3;
        } else {
            return 4;
        }
    }

    // Creates a Firebase authorization for specific user and sends email verification
    private void createUser() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send email verification
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verificationTask -> {
                                        if (verificationTask.isSuccessful()) {
                                            Toast.makeText(SignUp.this,
                                                    "Verification email sent to " + user.getEmail(),
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.e(TAG, "sendEmailVerification", verificationTask.getException());
                                            Toast.makeText(SignUp.this,
                                                    "Failed to send verification email.",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });

                            // Update user profile and save additional info
                            updateUserProfile(user);
                            saveAdditionalUserInfo(user);

                            // Notify user and redirect to login
                            Toast.makeText(SignUp.this, "User created successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUp.this, Login.class));
                            finish();
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUp.this,
                                "Authentication failed: " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    //Adds user data to Firebase
    private void updateUserProfile(FirebaseUser user) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                    }
                });
    }

    //Sets default data to complete all data requirements in User profile
    private void saveAdditionalUserInfo(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("dateFormat", "MM/dd/yyyy");
        userData.put("email", email);
        userData.put("name", name);
        userData.put("notifications", true);
        userData.put("notificationsFrequency", Arrays.asList(false, false, true, false));
        userData.put("timeFormat", "HH:mm");

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved successfully"))
                .addOnFailureListener(e -> Log.w(TAG, "Error saving user data", e));
    }


    //Uses built in email format validation
    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static class AsteriskPasswordTransformationMethod extends PasswordTransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return new PasswordCharSequence(source);
        }

        private static class PasswordCharSequence implements CharSequence {
            private CharSequence mSource;

            public PasswordCharSequence(CharSequence source) {
                mSource = source;
            }

            public char charAt(int index) {
                return '*';
            }

            public int length() {
                return mSource.length();
            }

            public CharSequence subSequence(int start, int end) {
                return mSource.subSequence(start, end);
            }
        }
    }
}
