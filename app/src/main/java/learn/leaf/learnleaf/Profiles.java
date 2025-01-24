package learn.leaf.learnleaf;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import learn.leaf.learnleaf.Firebase.OnProfileFetchedListener;


import java.util.List;

public class Profiles extends AppCompatActivity {

    private EditText nameEt;
    private EditText emailEt;
    private Spinner timeSpin;
    private Spinner dateSpin;
    private SwitchCompat notify;
    private Button updateBtn;
    private Profile user;
    private Firebase firebase;
    private ArrayAdapter<CharSequence> adapter1;
    private ArrayAdapter<CharSequence> adapter2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (vm, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            vm.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

        //firebase
        firebase = new Firebase(this);

        // Initialize views
        nameEt = findViewById(R.id.usernameXml);
        emailEt = findViewById(R.id.emailXml);
        timeSpin = findViewById(R.id.timeXml);
        dateSpin = findViewById(R.id.dateXml);
        notify = findViewById(R.id.notifySw);
        updateBtn = findViewById(R.id.updateBtn);
        user = new Profile("Not Set", "Not Set", "Not Set", false, "Not Set");


        updateBtn.setOnClickListener(v -> {
            //collect info from the editTexts and Spinners, compare it to the profile data that is
            //already set. If there are changes, update the database.
            String nameChange = nameEt.getText().toString();
            String emailChange = emailEt.getText().toString();
            String dateChange = dateSpin.getSelectedItem().toString();
            String timeChange = timeSpin.getSelectedItem().toString();
            Boolean notifyChange = notify.isChecked();

            if(nameChange == user.getName() && emailChange == user.getEmail()
                && dateChange == user.getProfileDate() && timeChange == user.getProfileTime()
                && notifyChange == user.getProfileNotify())
            {
                Toast.makeText(Profiles.this, "No Changes", Toast.LENGTH_SHORT).show();
            }
            else
            {
                if (!isValidEmail(emailChange) && !emailChange.isBlank()) {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                }
                else {
                    updateProfileDB(nameChange, emailChange, dateChange, timeChange, notifyChange);
                }
            }
        });

        // Sets up the status spinners
        adapter1 = ArrayAdapter.createFromResource(this,
                R.array.time_array, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpin.setAdapter(adapter1);
        int spinnerPosition1 = adapter1.getPosition(user.getProfileDate());
        timeSpin.setSelection(spinnerPosition1);

        adapter2 = ArrayAdapter.createFromResource(this,
                R.array.date_array, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateSpin.setAdapter(adapter2);
        int spinnerPosition2 = adapter2.getPosition(user.getProfileDate());
        dateSpin.setSelection(spinnerPosition2);

        fetchProfileData();

            return insets;
        });
    }

    private void updateProfileDB(String name, String email, String date, String time, Boolean notify) {
        firebase.updateProfileData(date, email, name, notify, time, new Firebase.OnProfileUpdatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Profiles.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(Profiles.this, "Failed to update profile: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void fetchProfileData() {
        firebase.fetchProfileData(new Firebase.OnProfileFetchedListener() {
            @Override
            public void onSuccess(Profile profile) {
                runOnUiThread(() -> {
                    // Update UI with profile data
                    updateProfileUI(profile);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(Profiles.this, "Failed to fetch user data: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("FetchProfileData", "Error fetching user data: " + errorMessage);
                });
            }
        });
    }

    private void updateProfileUI(Profile profile) {
        nameEt.setHint(profile.getName());
        emailEt.setHint(profile.getEmail());
        int spinnerPosition1 = adapter1.getPosition(profile.getProfileTime());
        timeSpin.setSelection(spinnerPosition1);
        int spinnerPosition2 = adapter2.getPosition(profile.getProfileDate());
        dateSpin.setSelection(spinnerPosition2);
        notify.setChecked(profile.getProfileNotify());
    }


    public static class Profile {

        private String profileName;
        private String profileEmail;
        private String profileTime;
        private String profileDate;
        private Boolean profileNotify;

    public Profile(){}

    public Profile(String date, String email, String name, Boolean notify, String time) {
        this.profileName = name;
        this.profileEmail = email;
        this.profileTime = time;
        this.profileDate = date;
        this.profileNotify = notify;
    }

        //Setters
        public void setProfileName(String name){this.profileName = name;}
        public void setProfileEmail(String email){this.profileEmail = email;}
        public void setProfileTime(String time){this.profileTime = time;}
        public void setProfileDate(String date){this.profileDate = date;}
        public void setProfileNotify(Boolean notify){this.profileNotify = notify;}

        // Getters

        public String getName() {
            return profileName;
        }

        public String getEmail() {
            return profileEmail;
        }
        public String getProfileTime() {
            return profileTime;
        }
        public String getProfileDate() {
            return profileDate;
        }
        public Boolean getProfileNotify(){
            return profileNotify;
        }
    }
}