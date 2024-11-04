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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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

    private void showCreateSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Subject");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_subject, null);
        final EditText subjectNameInput = viewInflated.findViewById(R.id.subjectNameInput);
        final EditText semesterInput = viewInflated.findViewById(R.id.semesterInput);
        final EditText colorInput = viewInflated.findViewById(R.id.colorInput);

        builder.setView(viewInflated);

        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok, null); // We'll set the listener later
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Set up a listener to be invoked when the dialog is shown
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false); // Initially disable the button

            // Set up a TextWatcher to monitor changes in the subjectNameInput
            subjectNameInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // Enable the button only if the subject name is not blank
                    positiveButton.setEnabled(s.toString().trim().length() > 0);
                }
            });

            // Set the click listener for the positive button
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

    private void showEditSubjectDialog(Subject subject) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Subject");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subject, null);
        final EditText subjectNameInput = viewInflated.findViewById(R.id.subjectNameInput);
        final EditText semesterInput = viewInflated.findViewById(R.id.semesterInput);
        final EditText colorInput = viewInflated.findViewById(R.id.colorInput);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // Pre-fill the fields with current subject data
        subjectNameInput.setText(subject.getSubjectName());
        semesterInput.setText(subject.getSemester());
        colorInput.setText(subject.getSubjectColor());

        // Set up the status spinner
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

    private void showDeleteConfirmationDialog(Subject subject, View blockView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Subject")
                .setMessage("Are you sure you want to delete this subject?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteSubject(subject, blockView))
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

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

            int color = parseColor(subject.getSubjectColor());
            cardView.setCardBackgroundColor(color);

            if (isColorDark(color)) {
                nameTextView.setTextColor(Color.WHITE);
                statusTextView.setTextColor(Color.WHITE);
                extraTextView.setTextColor(Color.WHITE);
            }

            String contentDescription = String.format("Subject: %s, Status: %s, Semester: %s",
                    subject.getSubjectName(), subject.getStatus(), subject.getSemester());
            cardView.setContentDescription(contentDescription);

            editButton.setOnClickListener(v -> showEditSubjectDialog(subject));
            deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(subject, blockView));

            subjectsContainer.addView(blockView);
        }
    }

    // Helper method to parse color string to color int
    private int parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            // Return a default color or throw a more specific exception
            return Color.BLACK; // or any other default color
        }
        try {
            return Color.parseColor(colorString);
        } catch (IllegalArgumentException e) {
            // Handle invalid color string
            Log.e("Subjects", "Invalid color string: " + colorString);
            return Color.BLACK; // or any other default color
        }
    }

    // Helper method to determine if a color is dark (for setting text color)
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
        private String subjectDescription;
        public String subjectId;


        // Default constructor (required for Firestore)
        public Subject() {}

        // Constructor with all fields
        public Subject(String semester, String status, String subjectColor, String subjectName) {
            this.subjectSemester = semester;
            this.subjectStatus = status;
            this.subjectColor = subjectColor;
            this.subjectName = subjectName;
        }

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

        public String getSubjectId() {
            return subjectId;
        }
    }


}