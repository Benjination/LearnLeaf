package com.example.learnleaf;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Projects extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout projectsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.projects);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        projectsContainer = findViewById(R.id.projectsContainer);

        fetchProjectsForCurrentUser();
    }

    private void fetchProjectsForCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();

        db.collection("projects")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Project> projects = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Project project = document.toObject(Project.class);
                        projects.add(project);
                    }
                    updateUI(projects);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Projects.this, "Failed to fetch projects.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(List<Project> projects) {
        if (projectsContainer == null) {
            Toast.makeText(Projects.this, "Null container", Toast.LENGTH_SHORT).show();
            return;
        }

        projectsContainer.removeAllViews();

        if (projects.isEmpty()) {
            TextView noProjectsText = new TextView(this);
            noProjectsText.setText("No projects found");
            projectsContainer.addView(noProjectsText);
            return;
        }

        for (Project project : projects) {
            View blockView = getLayoutInflater().inflate(R.layout.item_block, projectsContainer, false);

            CardView cardView = blockView.findViewById(R.id.itemCardView);
            TextView nameTextView = blockView.findViewById(R.id.itemNameTextView);
            TextView statusTextView = blockView.findViewById(R.id.itemStatusTextView);
            TextView extraTextView = blockView.findViewById(R.id.itemExtraTextView);
            ImageButton editButton = blockView.findViewById(R.id.editButton);
            ImageButton deleteButton = blockView.findViewById(R.id.deleteButton);

            nameTextView.setText(project.getProjectName());
            statusTextView.setText("Status: " + project.getStatus());
            extraTextView.setText("Subject: " + project.getSubject());

            String contentDescription = String.format("Project: %s, Status: %s, Subject: %s",
                    project.getProjectName(), project.getStatus(), project.getSubject());
            cardView.setContentDescription(contentDescription);

            editButton.setOnClickListener(v -> {
                // Handle edit action
            });

            deleteButton.setOnClickListener(v -> {
                // Handle delete action
                projectsContainer.removeView(blockView);
            });

            projectsContainer.addView(blockView);
        }
    }

    // Project class to represent the data model
    public static class Project {
        private String projectName;
        private String status;
        private String subject;
        private String userId;

        // Default constructor (required for Firestore)
        public Project() {}

        // Constructor with all fields
        public Project(String projectName, String status, String subject, String userId) {
            this.projectName = projectName;
            this.status = status;
            this.subject = subject;
            this.userId = userId;
        }

        // Getters and setters
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}