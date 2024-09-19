package com.example.learnleaf;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class Firebase {
    private static Firebase instance;
    private final FirebaseAuth mAuth;
    private final Context mContext;
    private final FirebaseFirestore db;
    private Firebase(Context context) {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        mContext = context.getApplicationContext();
    }
    //This is used in Sign Up page to Create New User
    public void createUser(String email, String password, Map<String, Object> userData, final AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Add user data to Firestore
                            db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "User data successfully written!");
                                        callback.onSuccess(firebaseUser);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w("Firestore", "Error writing user data", e);
                                        callback.onError("Failed to save user data: " + e.getMessage());
                                    });
                        } else {
                            callback.onError("Failed to get user after creation");
                        }
                    } else {
                        Log.w("FirebaseAuth", "createUserWithEmail:failure", task.getException());
                        callback.onError(task.getException() != null ? task.getException().getMessage() : "Unknown error occurred");
                    }
                });
    }

    //Ensures only one instance of Firebase is running
    public static synchronized Firebase getInstance(Context context) {
        if (instance == null) {
            instance = new Firebase(context);
        }
        return instance;
    }

    //Used in Login page to check the input email and password, and Sign in
    public void signIn(String email, String password, final AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    //Used on Home page to log off user
    public void signOut() {
        mAuth.signOut();
        Toast.makeText(mContext, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    //Collects user information of currently signed in user from Firestore
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }
}