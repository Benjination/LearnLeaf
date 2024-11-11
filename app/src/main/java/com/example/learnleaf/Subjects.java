package com.example.learnleaf;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import java.lang.reflect.Field;
import java.util.List;

public class Subjects extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private static LinearLayout subjectsContainer;
    private ImageView addnew;
    private Firebase firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subjects);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        subjectsContainer = findViewById(R.id.subjectsContainer);
        addnew = findViewById(R.id.addnew);
        firebase = new Firebase(this);
        addnew.setOnClickListener(v -> showCreateSubjectDialog());
        fetchAllSubjectsForCurrentUser();
    }

    //Initiated by pressing addNew button, Allows user to create a new Subject
    private void showCreateSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Subject");

        //opens dialog_create_subject
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_subject, null);
        final EditText subjectNameInput = viewInflated.findViewById(R.id.subjectNameInput);
        final EditText semesterInput = viewInflated.findViewById(R.id.semesterInput);
        final EditText colorInput = viewInflated.findViewById(R.id.colorInput);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, null); // We'll set the listener later
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false); // Initially disable the button

            //This will not allow a user to submit new Subject if subject name is blank
            subjectNameInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    positiveButton.setEnabled(s.toString().trim().length() > 0);
                }
            });

            positiveButton.setOnClickListener(v -> {
                String subjectName = subjectNameInput.getText().toString().trim();
                String semester = semesterInput.getText().toString().trim();
                String color = colorInput.getText().toString().trim();
                createNewSubject(subjectName, semester, color);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    //Edit button function - Allows user to edit existing Subjects
    private void showEditSubjectDialog(Subject subject) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Subject");

        //opens dialog_edit_subject
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subject, null);
        final EditText subjectNameInput = viewInflated.findViewById(R.id.subjectNameInput);
        final EditText semesterInput = viewInflated.findViewById(R.id.semesterInput);
        final EditText colorInput = viewInflated.findViewById(R.id.colorInput);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        //Fills dialog boxes with existing data
        subjectNameInput.setText(subject.getSubjectName());
        semesterInput.setText(subject.getSemester());
        colorInput.setText(subject.getSubjectColor());

        //Sets up the status spinner - array is in res>values>arrays.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
        int spinnerPosition = adapter.getPosition(subject.getStatus());
        statusSpinner.setSelection(spinnerPosition);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String subjectName = subjectNameInput.getText().toString();
            String semester = semesterInput.getText().toString();
            String color = colorInput.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();
            updateSubject(subject, subjectName, semester, color, status);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    //Call to method in Firebase.java
    private void updateSubject(Subject subject, String subjectName, String semester, String color, String status) {
        firebase.updateSubject(subject, subjectName, semester, color, status, new Firebase.OnSubjectUpdatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Subjects.this, "Subject updated successfully", Toast.LENGTH_SHORT).show();
                fetchAllSubjectsForCurrentUser(); // Refresh the list
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Call to method in Firebase.java
    private void createNewSubject(String subjectName, String semester, String color) {
        firebase.createNewSubject(subjectName, semester, color, new Firebase.OnSubjectCreatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Subjects.this, "Subject created successfully", Toast.LENGTH_SHORT).show();
                fetchAllSubjectsForCurrentUser(); // Refresh the list
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Call to method in Firebase.java
    private void deleteSubject(Subject subject, View blockView) {
        firebase.deleteSubject(subject, new Firebase.OnSubjectDeletedListener() {
            @Override
            public void onSuccess() {
                subjectsContainer.removeView(blockView);
                Toast.makeText(Subjects.this, "Subject deleted successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Checks with user before permanantly deleting a subject
    private void showDeleteConfirmationDialog(Subject subject, View blockView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Subject")
                .setMessage("Are you sure you want to delete this subject?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteSubject(subject, blockView))
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    //Call to method in Firebase.java
    private void fetchAllSubjectsForCurrentUser() {
        firebase.fetchAllSubjectsForCurrentUser(new Firebase.OnAllSubjectsFetchedListener() {
            @Override
            public void onSuccess(List<Subject> allSubjects) {
                updateUI(allSubjects);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //After fetching new list of Subjects, this method will update the item_blocks in the Scrollview
    public void updateUI(List<Subject> subjects) {
        if (subjectsContainer == null) {
            Toast.makeText(Subjects.this, "Null container", Toast.LENGTH_SHORT).show();
            return;
        }

        subjectsContainer.removeAllViews();

        if (subjects.isEmpty()) {
            TextView noSubjectsText = new TextView(this);
            noSubjectsText.setText("No subjects found");
            subjectsContainer.addView(noSubjectsText);
            return;
        }

        for (Subject subject : subjects) {
            //creates item block for each subject in subject list
            View blockView = getLayoutInflater().inflate(R.layout.item_block, subjectsContainer, false);

            CardView cardView = blockView.findViewById(R.id.itemCardView);
            TextView nameTextView = blockView.findViewById(R.id.itemNameTextView);
            TextView statusTextView = blockView.findViewById(R.id.itemStatusTextView);
            TextView extraTextView = blockView.findViewById(R.id.itemExtraTextView);
            ImageButton editButton = blockView.findViewById(R.id.editButton);
            ImageButton deleteButton = blockView.findViewById(R.id.deleteButton);

            nameTextView.setText(subject.getSubjectName());
            statusTextView.setText("Status: " + subject.getStatus());
            extraTextView.setText("Semester: " + subject.getSemester());

            // Debug log to check the color value
            Log.d("UpdateUI", "Subject: " + subject.getSubjectName() + ", Color: " + subject.getSubjectColor());

            int color = parseColor(subject.getSubjectColor());
            cardView.setCardBackgroundColor(color);

            //Adjusts text color to provide contrast on user selected background color
            if (isColorDark(color)) {
                nameTextView.setTextColor(Color.WHITE);
                statusTextView.setTextColor(Color.WHITE);
                extraTextView.setTextColor(Color.WHITE);
            } else {
                nameTextView.setTextColor(Color.BLACK);
                statusTextView.setTextColor(Color.BLACK);
                extraTextView.setTextColor(Color.BLACK);
            }

            String contentDescription = String.format("Subject: %s, Status: %s, Semester: %s",
                    subject.getSubjectName(), subject.getStatus(), subject.getSemester());
            cardView.setContentDescription(contentDescription);

            editButton.setOnClickListener(v -> showEditSubjectDialog(subject));
            deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(subject, blockView));

            subjectsContainer.addView(blockView);
        }
    }

    // Improved parseColor method
    private int parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            Log.w("UpdateUI", "Empty or null color string, using default");
            return Color.LTGRAY; // Default color
        }
        try {
            if (colorString.startsWith("#")) {
                return Color.parseColor(colorString);
            } else {
                Field field = Color.class.getField(colorString.toUpperCase());
                return field.getInt(null);
            }
        } catch (Exception e) {
            Log.e("UpdateUI", "Error parsing color: " + colorString, e);
            return Color.LTGRAY; //Default color if color cannot be found
        }
    }

    //Determines if Color is "Dark"
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    // Subject class to represent the data model
    public static class Subject {

        private String subjectSemester;
        private String subjectStatus;
        private String subjectColor;
        private String subjectName;
        private String subjectDescription; //Will have eventually
        public String subjectId;


        //Empty constructor required by Firebase
        //Do not delete
        public Subject() {}

        public Subject(String semester, String status, String subjectColor, String subjectName) {
            this.subjectSemester = semester;
            this.subjectStatus = status;
            this.subjectColor = subjectColor;
            this.subjectName = subjectName;
        }

        //Setters
        public void setSubjectColor(String color){this.subjectColor = color;}
        public void setSubjectId(String documentId) {this.subjectId = documentId;}
        public void setSubjectName(String newSubjectName) {this.subjectName = newSubjectName;}
        public void setSemester(String newSemester) {this.subjectSemester = newSemester;}
        public void setStatus(String newStatus) {this.subjectStatus = newStatus;}

        // Getters
        @PropertyName("subjectSemester")
        public String getSemester() {
            return subjectSemester;
        }
        @PropertyName("subjectStatus")
        public String getStatus() {
            return subjectStatus;
        }
        public String getSubjectColor() {
            return subjectColor;
        }
        public String getSubjectName() {
            return subjectName;
        }
        public String getSubjectId() {return subjectId;}
    }
}