package com.example.learnleaf;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Subjects extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout subjectsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.subjects);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize the subjectsContainer
        subjectsContainer = findViewById(R.id.subjectsContainer);

        // Call the method to fetch active subjects for the current user
        fetchActiveSubjectsForCurrentUser();
    }

    private void fetchActiveSubjectsForCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Handle the case where no user is signed in
            return;
        }

        String userId = currentUser.getUid();

        db.collection("subjects")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Subject> activeSubjects = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Subject subject = document.toObject(Subject.class);
                        activeSubjects.add(subject);
                    }

                    // Here you have the list of active subjects for the current user
                    // You can update your UI or do further processing here
                    updateUI(activeSubjects);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Subjects.this, "Failed to fetch subjects.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(List<Subject> subjects) {
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

            editButton.setOnClickListener(v -> {
                // Handle edit action
            });

            deleteButton.setOnClickListener(v -> {
                // Handle delete action
                subjectsContainer.removeView(blockView);
            });

            subjectsContainer.addView(blockView);
        }
    }

    // Helper method to parse color string to color int
    private int parseColor(String colorString) {
        try {
            return Color.parseColor(colorString);
        } catch (IllegalArgumentException e) {
            // Return a default color if parsing fails
            return ContextCompat.getColor(this, android.R.color.black);
        }
    }

    // Helper method to determine if a color is dark (for setting text color)
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    // Subject class to represent the data model
    public static class Subject {
        private String semester;
        private String status;
        private String subjectColor;
        private String subjectName;
        private String userId;

        // Default constructor (required for Firestore)
        public Subject() {}

        // Constructor with all fields
        public Subject(String semester, String status, String subjectColor, String subjectName, String userId) {
            this.semester = semester;
            this.status = status;
            this.subjectColor = subjectColor;
            this.subjectName = subjectName;
            this.userId = userId;
        }

        // Getters
        public String getSemester() {
            return semester;
        }

        public String getStatus() {
            return status;
        }

        public String getSubjectColor() {
            return subjectColor;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public String getUserId() {
            return userId;
        }

        // Setters
        public void setSemester(String semester) {
            this.semester = semester;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setSubjectColor(String subjectColor) {
            this.subjectColor = subjectColor;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        // Optional: Override toString() method for easy printing/debugging
        @Override
        public String toString() {
            return "Subject{" +
                    "semester='" + semester + '\'' +
                    ", status='" + status + '\'' +
                    ", subjectColor='" + subjectColor + '\'' +
                    ", subjectName='" + subjectName + '\'' +
                    ", userId='" + userId + '\'' +
                    '}';
        }
    }


}