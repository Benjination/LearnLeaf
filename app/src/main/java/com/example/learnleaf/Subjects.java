package com.example.learnleaf;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

        // Ensure subjectsContainer is not null
       // if (subjectsContainer == null) {
         //   throw new RuntimeException("subjectsContainer not found in layout");
        //}

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
                    Toast.makeText(Subjects.this, "Currently there are no active Subjects.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(List<Subject> subjects) {
        if (subjectsContainer == null) {
            Toast.makeText(Subjects.this, "Null container", Toast.LENGTH_SHORT).show();
            return;
        }


        // Clear any existing views in the container
        subjectsContainer.removeAllViews();

        if (subjects.isEmpty()) {
            TextView noSubjectsText = new TextView(this);
            noSubjectsText.setText("No subjects found");
            subjectsContainer.addView(noSubjectsText);
        }

        // Add a TextView for each subject
        for (Subject subject : subjects) {
            TextView textView = new TextView(this);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setPadding(0, 16, 0, 16); // Add some vertical padding

            // Set the text for the TextView
            String subjectText = subject.getSubjectName();
            textView.setText(subjectText);

            // Set the text color based on the subject color
            int color = parseColor(subject.getSubjectColor());
            textView.setTextColor(color);

            // Set content description for accessibility
            String contentDescription = String.format("Subject: %s, Semester: %s, Status: %s",
                    subject.getSubjectName(), subject.getSemester(), subject.getStatus());
            textView.setContentDescription(contentDescription);

            // Ensure the view is accessible to screen readers
            textView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            // Add the TextView to the container
            subjectsContainer.addView(textView);
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