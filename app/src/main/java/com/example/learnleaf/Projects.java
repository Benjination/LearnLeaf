package com.example.learnleaf;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Projects extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout projectsContainer;
    private Firebase firebase;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.projects);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebase = new Firebase(this);
        projectsContainer = findViewById(R.id.projectsContainer);
        ImageView addnew = findViewById(R.id.addnew);

        addnew.setOnClickListener(v -> showCreateProjectDialog());

        fetchProjectsForCurrentUser();
    }

    private void showCreateProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Project");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_project, null);
        final EditText projectNameInput = viewInflated.findViewById(R.id.projectNameInput);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // Set up the status spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String projectName = projectNameInput.getText().toString();
            String subject = subjectInput.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();
            createNewProject(projectName, subject, status);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createNewProject(String projectName, String subject, String status) {
        firebase.createNewProject(projectName, subject, status, new Firebase.OnProjectCreatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Projects.this, "Project created successfully", Toast.LENGTH_SHORT).show();
                fetchProjectsForCurrentUser(); // Refresh the list
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Projects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchProjectsForCurrentUser() {
        firebase.fetchProjectsForCurrentUser(new Firebase.OnProjectsFetchedListener() {
            @Override
            public void onSuccess(List<Project> projects) {
                updateUI(projects);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Projects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showEditProjectDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Project");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_project, null);
        final EditText projectNameInput = viewInflated.findViewById(R.id.projectNameInput);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // Pre-fill the fields with current project data
        projectNameInput.setText(project.getProjectName());
        subjectInput.setText(project.getSubject());

        // Set up the status spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
        int spinnerPosition = adapter.getPosition(project.getStatus());
        statusSpinner.setSelection(spinnerPosition);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String projectName = projectNameInput.getText().toString();
            String subject = subjectInput.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();
            updateProject(project.projectName, projectName, subject, status);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void deleteProject(String projectName, View blockView) {
        FirebaseUser currentUser = firebase.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        firebase.deleteProject(projectName, userId, new Firebase.OnProjectDeletedListener() {
            @Override
            public void onSuccess() {
                projectsContainer.removeView(blockView);
                Toast.makeText(Projects.this, "Project deleted successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Projects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showDeleteConfirmationDialog(Project project, View blockView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete this project?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteProject(project.getProjectName(), blockView))
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void updateProject(String currentProjectName, String newProjectName, String newSubject, String newStatus) {
        firebase.updateProject(currentProjectName, newProjectName, newSubject, newStatus, new Firebase.OnProjectUpdatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Projects.this, "Project updated successfully", Toast.LENGTH_SHORT).show();
                fetchProjectsForCurrentUser(); // Refresh the list
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Projects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
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
                showEditProjectDialog(project);
            });
            deleteButton.setOnClickListener(v -> {
                showDeleteConfirmationDialog(project, blockView);
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