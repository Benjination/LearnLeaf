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
import java.util.Arrays;
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
        final EditText projectDescriptionInput = viewInflated.findViewById(R.id.projectDescriptionInput); // New input for description
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput); // Assuming this is for subjects

        // Set up the status spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String projectName = projectNameInput.getText().toString();
            String projectDescription = projectDescriptionInput.getText().toString(); // Get description
            String status = statusSpinner.getSelectedItem().toString();

            // Assuming subjects are entered as a comma-separated string
            String subjectString = subjectInput.getText().toString();
            List<String> projectSubjects = Arrays.asList(subjectString.split(",\\s*")); // Convert to List

            createNewProject(projectName, projectDescription, status, projectSubjects); // Updated call
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createNewProject(String projectName, String projectDescription, String projectStatus, List<String> projectSubjects) {
        firebase.createNewProject(projectName, projectDescription, projectStatus, projectSubjects, new Firebase.OnProjectCreatedListener() {
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
        final EditText projectDescriptionInput = viewInflated.findViewById(R.id.projectDescriptionInput); // New input for description
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // Pre-fill the fields with current project data
        projectNameInput.setText(project.getProjectName());
        projectDescriptionInput.setText(project.getProjectDescription()); // Set description

        // Set up the status spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        int spinnerPosition = adapter.getPosition(project.getProjectStatus());
        statusSpinner.setSelection(spinnerPosition);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String projectName = projectNameInput.getText().toString();
            String projectDescription = projectDescriptionInput.getText().toString(); // Get description
            String status = statusSpinner.getSelectedItem().toString();

            // Assuming that you no longer need subjects input
            List<String> projectSubjects = new ArrayList<>(); // Initialize an empty list or modify as needed

            // Use the document ID directly from the project object if needed
            updateProject(project.getDocumentId(), projectName, projectDescription, status, projectSubjects); // Updated call
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

        // Use only the project name to delete
        firebase.deleteProject(projectName, new Firebase.OnProjectDeletedListener() {
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

    public void updateProject(String projectId, String newProjectName, String newProjectDescription, String newStatus, List<String> newSubjects) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Handle user not signed in
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("projects").document(projectId)
                .update(
                        "projectName", newProjectName,
                        "projectDescription", newProjectDescription,
                        "projectStatus", newStatus,
                        "projectSubjects", newSubjects // Update subjects
                )
                .addOnSuccessListener(aVoid -> {
                    // Handle success
                })
                .addOnFailureListener(e -> {
                    // Handle failure
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

            // Set project name and status
            nameTextView.setText(project.getProjectName());
            statusTextView.setText("Status: " + project.getProjectStatus());

            // Display subjects as a comma-separated string
            String subjectsString = String.join(", ", project.getProjectSubjects());
            extraTextView.setText("Subjects: " + subjectsString);

            String contentDescription = String.format("Project: %s, Status: %s, Subjects: %s",
                    project.getProjectName(), project.getProjectStatus(), subjectsString);
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

    public static class Project {
        private String projectName;
        private String projectDescription; // New attribute
        private String projectStatus; // New attribute
        private List<String> projectSubjects; // New attribute
        private String documentId;

        // No-argument constructor
        public Project() {
            this.projectSubjects = new ArrayList<>(); // Initialize as an empty list
            this.projectDescription = ""; // Default value
            this.projectName = ""; // Default value
            this.projectStatus = "Active"; // Default value (or adjust as needed)
        }

        // Constructor with parameters
        public Project(String projectName, String projectDescription, String projectStatus) {
            this.projectName = projectName;
            this.projectDescription = projectDescription;
            this.projectStatus = projectStatus;
            this.projectSubjects = new ArrayList<>(); // Initialize as an empty list

        }

        // Getters and Setters
        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getProjectDescription() {
            return projectDescription;
        }

        public void setProjectDescription(String projectDescription) {
            this.projectDescription = projectDescription;
        }

        public String getProjectStatus() {
            return projectStatus;
        }

        public void setProjectStatus(String projectStatus) {
            this.projectStatus = projectStatus;
        }

        public List<String> getProjectSubjects() {
            return projectSubjects;
        }

        public void setProjectSubjects(List<String> subjects) {
            this.projectSubjects = subjects;
        }

        public String getDocumentId() {
            return documentId;
        }

        public void setDocumentId(String documentId) {
            this.documentId = documentId; // Implement this method
        }
    }
}