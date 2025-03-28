package learn.leaf.learnleaf;

// Basics
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

//Credential management tools
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPasswordOption;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.webauthn.PublicKeyCredentialCreationOptions;
//import androidx.credentials.SaveCredentialRequest;
import androidx.credentials.PublicKeyCredential;

import android.os.CancellationSignal;

// Google identity tools
//import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

//Concurrency stuff
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

//Widget tools
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;


//-> sign in with password
//onCreate -> getAvailable ->

// Pseudo code
// pull available credential options
// if no credential options are available, generate new credential with password
// save new credential
// initiate sign in with new credential

// SIGN IN FLOW
//    - open authentication screen (bottom sheet ideally) (bottom sheet is credential manager?)
//    - once the user signs in, we get a credential object that we can use to create id token
//    -
//    - create instance of cred manager
//    - use sign in to get a credential object
//    - create ID token with credential
//    - pass token to firebase
//    -


public class Login extends AppCompatActivity {


    //===================================================
    //==================== F I E L D S ==================
    //===================================================
    private static final String TAG = "LoginActivity";

    //create credential manager instance
    private CredentialManager credentialManager;
    private FirebaseAuth mAuth;
    private ExecutorService executorService;
    private EditText email, password;
    private Button submit, back;
    private TextView forgot;


    private String requestJson = "google-services.json";//might not be necessary
/*
    // Retrieves the user's saved password for your app from their password provider.
    GetPasswordOption getPasswordOption = new GetPasswordOption();

    // Get passkey from the user's public key credential provider.
    GetPublicKeyCredentialOption getPublicKeyGoogleOption =
            new GetPublicKeyCredentialOption("google-services.json");
*/
    // TODO: create google sign in credential object with data from google-services.json
    // val googleIdOption = GetGoogleIdOption.builder()
    //        .SetFilterByAuthorizedAccounts(hasFilter)
    //retrieveSavedCredentials();


    //=======================================================
    //================== M E T H O D S ======================
    //=======================================================

    //===========================
    //==== INITIALIZE VIEWS =====
    //===========================
    private void initializeViews() {
        back = findViewById(R.id.back);
        submit = findViewById(R.id.submit);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        forgot = findViewById(R.id.fgPassword);///// TODO: FIGURE OUT WHAT THIS IS FOR
    }


    // ============================
    // ==== SET LISTENERS =========
    // ============================
    private void setListeners(){
        back.setOnClickListener(v -> {
            startActivity(new Intent(Login.this, SignUp.class));
            finish();
        });
        submit.setOnClickListener(v -> signInWithPassword( email.getText().toString().trim(),
         password.getText().toString().trim()));
        //forgot.setOnClickListener(v -> forgotPassword());
    }


    // ============================
    // ========= ON CREATE ========
    // ============================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Create the code from the parent and set up the image
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        //Create the instance of the credential manager, executor, and firebase auth
        credentialManager = CredentialManager.create(this);
        executorService = Executors.newSingleThreadExecutor();
        mAuth = FirebaseAuth.getInstance();

        //set the stage for login page
        initializeViews();
        setListeners();

        showCredentialManager();
    }//end of onCreate


    // ===========================================
    // ===== SHOW CREDENTIAL MANAGER =============
    // ===========================================
    private void showCredentialManager() {

        // Flow is: option -> request -> get -> response

        // INIT credential sign in options
        GetPasswordOption passwordOption = new GetPasswordOption();
        //TODO: FIGURE OUT WHAT TO DO ABOUT REQUEST.JSON
        GetPublicKeyCredentialOption getPublicKeyCredentialOption =
                new GetPublicKeyCredentialOption(requestJson);

        // INIT GetCredentialRequest (to show the credential manager UI hopefully)
        GetCredentialRequest getCredRequest = new GetCredentialRequest.Builder()
                .addCredentialOption(passwordOption)
                .addCredentialOption(getPublicKeyCredentialOption)
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
    public void handleSignIn(GetCredentialResponse result) {
        // Handle the successfully returned credential.
        Credential credential = result.getCredential();
        if (credential instanceof PublicKeyCredential) {
            String responseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();

            // Share responseJson i.e. a GetCredentialResponse on your server to validate and authenticate
        } else if (credential instanceof PasswordCredential) {
            String username = ((PasswordCredential) credential).getId();
            String password = ((PasswordCredential) credential).getPassword();
            signInWithPassword(username,password);
            //TODO: insert code for passkey login here
        } else {
            // Catch any unrecognized credential type here.
            Log.e(TAG, "Unexpected type of credential");
        }
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
    private void initializeViews() {
        back = findViewById(R.id.back);
        submit = findViewById(R.id.submit);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        forgot = findViewById(R.id.fgPassword);
    }
        */



}//end

