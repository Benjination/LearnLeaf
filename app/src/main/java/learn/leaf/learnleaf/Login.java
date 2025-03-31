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

<<<<<<< HEAD
import android.os.CancellationSignal;

// Google identity tools
//import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
=======
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
>>>>>>> parent of b283b71 (attempted sso)
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

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
<<<<<<< HEAD

        showCredentialManager();
    }//end of onCreate


    // ===========================================
    // ===== SHOW CREDENTIAL MANAGER =============
    // ===========================================
    private void showCredentialManager() {
        // Initialize Google Sign-In option
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId("YOUR_WEB_CLIENT_ID") // From Google Cloud Console
                .setFilterByAuthorizedAccounts(true)
                .build();

        // Initialize other credential options
        GetPasswordOption passwordOption = new GetPasswordOption();
        GetPublicKeyCredentialOption publicKeyOption =
                new GetPublicKeyCredentialOption(requestJson);

        // Build credential request
        GetCredentialRequest getCredRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .addCredentialOption(passwordOption)
                .addCredentialOption(publicKeyOption)
                .build();

        // INIT cancellation signal object
        CancellationSignal cancellationSignal = new CancellationSignal();

        //CREATE get credential request
        credentialManager.getCredentialAsync(
                // Use activity based context to avoid undefined
                // system UI launching behavior
                Login.this,
                getCredRequest,
                cancellationSignal,
                executorService,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        e.printStackTrace();
                        System.out.println(e);
                    }
                });

/*
        // Show the UI for credential manager
        credentialManager.getCredential(getCredRequest)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        GetCredentialResponse response = task.getResult();
                        // Handle the response, like showing the credentials
                    } else {
                        // Handle failure (e.g., no saved credentials, user dismissed the UI)
                        GetCredentialException exception = (GetCredentialException) task.getException();
                        if (exception != null) {
                            // Handle the exception accordingly
                        }
                    }
                };
                */

    }

    //======================
    //== HANDLE SIGN IN ====
    //======================
    private void handleSignIn(GetCredentialResponse response) {
        Credential credential = response.getCredential();

        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                try {
                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(customCredential.getData());

                    // Get the ID token
                    String idToken = googleCredential.getIdToken();

                    // Authenticate with Firebase
                    AuthCredential firebaseCredential =
                            GoogleAuthProvider.getCredential(idToken, null);
                    mAuth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this, task -> {
                                if (task.isSuccessful()) {
                                    // Handle successful login
                                } else {
                                    // Handle failure
                                }
                            });

                } catch (GoogleIdTokenParsingException e) {
                    Log.e(TAG, "Error parsing Google ID token", e);
                }
            }
        }
        // Handle other credential types...
    }



    //--------------------------------------------------------
    // --------------- SIGN IN WITH PASSWORD -----------------
    //--------------------------------------------------------
    private void signInWithPassword(String userEmail, String userPassword) {
        //capture email and password from interface
        //String userEmail = email.getText().toString().trim();
        //String userPassword = password.getText().toString().trim();

        //check that input is Valid
        if (userEmail.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(Login.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

         mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                 .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("Auth", "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if the user's email is verified
                            if (user.isEmailVerified()) {
                                updateUI(user); // Proceed to the next activity or update UI
                            } else {
                                Toast.makeText(Login.this,
                                        "Please verify your email",
                                        Toast.LENGTH_LONG).show();
                                mAuth.signOut(); // Sign out the user to prevent access
                            }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(Login.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                 });
    }// END OF SIGN ON WITH PASSWORD


    //====================
    //===== UPDATE UI ====
    //====================
    private void updateUI(FirebaseUser user){
        if (user != null) {
            Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Login.this, Tasks.class));
            finish();
        }
    }


    //private void createPasskeyCredential() {
        //public PublicKeyCredentialCreationOptions ("google-services.json")
    //}


    //--------------------------------------------------------
    // --------------- RETRIEVE SAVED CREDENTIALS ------------
    //--------------------------------------------------------
    /*
    // TODO: finish this and understand how it works, generates credential option object and passes to get credential to handle
    private void retrieveSavedCredentials() {
        executorService.execute(() -> {
            try {
                GetCredentialRequest request = new GetCredentialRequest.Builder()
                        .addCredentialOption(new PasswordCredentialOption()) //TODO: passwordCredentialOption depreciated
                        //TODO: add credential option for google sign in
                        .build();

                credentialManager.getCredential(this, request)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                handleRetrievedCredential(task.getResult().getCredential());
                            } else {
                                Log.e(TAG, "No saved credentials found", task.getException());
                            }
                        });
            } catch (GetCredentialException e) {
                Log.e(TAG, "Failed to retrieve credentials", e);
            }
        });
    }// END OF RETRIEVE SAVED CREDENTIALS

    */







    /*
}
        // Simulate authentication (replace with Firebase or backend authentication)
        boolean loginSuccess = mockAuthenticate(userEmail, userPassword);

        if (loginSuccess) {
            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
            saveCredentials(userEmail, userPassword);
        } else {
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();



    private boolean mockAuthenticate(String email, String password) {
        // Replace with actual authentication logic (e.g., Firebase)
        return "test@example.com".equals(email) && "password123".equals(password);
    }







    // -------------------- SAVE CREDENTIALS -----------------------
    private void saveCredentials(String userEmail, String userPassword) {
        executorService.execute(() -> {
            try {
                // Build the request to save the credentials
                CreatePasswordRequest request = new CreatePasswordRequest(userEmail, userPassword);

                // Save the credentials using Credential Manager
                credentialManager.createCredential(this, request, executorService)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Credentials saved successfully");
                            } else {
                                Log.e(TAG, "Failed to save credentials", task.getException());
                            }
                        });

            } catch (CreateCredentialException e) {
                Log.e(TAG, "Error saving credentials", e);
            }
        });
    }







    private void signIn() {
        signInClient.beginSignIn(signInRequest)
                .addOnCompleteListener(new OnCompleteListener<BeginSignInRequest>() {
                    @Override
                    public void onComplete(@NonNull Task<BeginSignInRequest> task) {
                        if (task.isSuccessful()) {
                            Intent signInIntent = task.getResult().getPendingIntent().getIntent();
                            signInLauncher.launch(signInIntent);
                        } else {
                            Log.e(TAG, "Sign-in request failed", task.getException());
                        }
                    }
                });
    }






    private void authenticateWithBackend(String idToken) {
        // Send the idToken to the backend for authentication.
        Log.d(TAG, "Google ID Token: " + idToken);
    }




*/




        /*
=======
    }

>>>>>>> parent of b283b71 (attempted sso)
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
