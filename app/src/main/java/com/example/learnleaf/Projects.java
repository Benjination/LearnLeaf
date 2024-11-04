package com.example.learnleaf;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.ParseException;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        } else {
            // Handle the case where no user is signed in
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void showCreateProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Project");

        String userId = getCurrentUserId();

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_project, null);
        final EditText projectNameInput = viewInflated.findViewById(R.id.projectNameInput);
        final EditText projectDescriptionInput = viewInflated.findViewById(R.id.projectDescriptionInput);
        final Button projectDueDateButton = viewInflated.findViewById(R.id.dueDateButton);
        final Button projectDueTimeButton = viewInflated.findViewById(R.id.dueTimeButton);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);

        // Set up the status spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        // Initialize date and time
        final Calendar calendar = Calendar.getInstance();
        final Date[] projectDueDate = {null};
        final Date[] projectDueTime = {null};

        // Set up date picker
        projectDueDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        projectDueDate[0] = calendar.getTime();
                        projectDueDateButton.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(projectDueDate[0]));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // Set up time picker
        projectDueTimeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        projectDueTime[0] = calendar.getTime();
                        projectDueTimeButton.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(projectDueTime[0]));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePickerDialog.show();
        });

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String projectName = projectNameInput.getText().toString();
            String projectDescription = projectDescriptionInput.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();

            String subjectString = subjectInput.getText().toString();
            List<String> projectSubjects = Arrays.asList(subjectString.split(",\\s*"));

            // Convert subject strings to DocumentReferences
            List<DocumentReference> subjectRefs = new ArrayList<>();
            for (String subject : projectSubjects) {
                subjectRefs.add(db.collection("users").document(userId).collection("subjects").document(subject));
            }

            createNewProject(projectName, projectDescription, status, subjectRefs, projectDueDate[0], projectDueTime[0]);
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createNewProject(String projectName, String projectDescription, String projectStatus,
                                  List<DocumentReference> projectSubjects, Date projectDueDate, Date projectDueTime) {
        firebase.createNewProject(projectName, projectDescription, projectStatus, projectSubjects,projectDueDate,
                projectDueTime, new Firebase.OnProjectCreatedListener() {
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
            String newProjectName = projectNameInput.getText().toString();
            String projectDescription = projectDescriptionInput.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();

            // Pass an empty list for subjects if you're not handling them in the dialog
            List<String> projectSubjects = new ArrayList<>();

            // Call the updateProject method
            updateProject(project.getProjectName(), newProjectName, projectDescription, status, projectSubjects);// Updated call
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

    public void updateProject(String oldProjectName, String newProjectName, String projectDescription, String projectStatus, List<String> subjectIds) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Handle the case where the user is not signed in
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Query to find the project document by its name
        db.collection("users").document(userId).collection("projects")
                .whereEqualTo("name", oldProjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Assuming project names are unique, get the first document
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String projectId = documentSnapshot.getId();

                        // Create a map to hold the updated fields
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", newProjectName);
                        updates.put("description", projectDescription);
                        updates.put("status", projectStatus);

                        // Convert subject IDs to DocumentReferences if needed
                        if (!subjectIds.isEmpty()) {
                            List<DocumentReference> subjectReferences = new ArrayList<>();
                            for (String subjectId : subjectIds) {
                                DocumentReference subjectRef = db.collection("users").document(userId).collection("subjects").document(subjectId);
                                subjectReferences.add(subjectRef);
                            }
                            updates.put("projectSubjects", subjectReferences);
                        }

                        // Update the existing project in Firestore
                        db.collection("users").document(userId).collection("projects").document(projectId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();
                                    // Refresh your UI or project list here
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error updating project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error finding project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        String userId = getCurrentUserId();

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (Project project : projects) {
            View blockView = getLayoutInflater().inflate(R.layout.item_block, projectsContainer, false);

            CardView cardView = blockView.findViewById(R.id.itemCardView);
            TextView nameTextView = blockView.findViewById(R.id.itemNameTextView);
            TextView statusTextView = blockView.findViewById(R.id.itemStatusTextView);
            TextView extraTextView = blockView.findViewById(R.id.itemExtraTextView);
            TextView dueDateTextView = blockView.findViewById(R.id.itemDueDateTextView);
            TextView dueTimeTextView = blockView.findViewById(R.id.itemDueTimeTextView);
            ImageButton editButton = blockView.findViewById(R.id.editButton);
            ImageButton deleteButton = blockView.findViewById(R.id.deleteButton);

            // Set project name and status
            nameTextView.setText(project.getProjectName());
            statusTextView.setText("Status: " + project.getProjectStatus());

            Date dueDate = project.getProjectDueDate();
            Date dueTime = project.getProjectDueTime();

            if (dueDate != null) {
                dueDateTextView.setText("Due: " + dateFormat.format(dueDate));
            } else {
                dueDateTextView.setText("Due: Not set");
            }

            if (dueTime != null) {
                dueTimeTextView.setText("Time: " + timeFormat.format(dueTime));
            } else {
                dueTimeTextView.setText("Time: Not set");
            }

            // Convert DocumentReferences to strings (using their IDs)
            List<String> subjectIds = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                subjectIds = project.getProjectSubjects().stream()
                        .map(DocumentReference::getId)
                        .collect(Collectors.toList());
            }

            // Now join the string IDs
            String subjectsString = String.join(", ", subjectIds);
            extraTextView.setText("Subjects: " + subjectsString);

            String contentDescription = String.format("Project: %s, Status: %s, Subjects: %s, Due Date: %s, Due Time: %s",
                    project.getProjectName(), project.getProjectStatus(), subjectsString,
                    dueDate != null ? dateFormat.format(dueDate) : "Not set",
                    dueTime != null ? timeFormat.format(dueTime) : "Not set");
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
        private String projectId;
        private String projectName;
        private String projectDescription;
        private String projectStatus;
        private List<DocumentReference> projectSubjects;
        private Date projectDueDate;
        private Date projectDueTime;

        // No-argument constructor
        public Project() {
            this.projectSubjects = new ArrayList<>();
            this.projectDescription = "";
            this.projectName = "None";
            this.projectStatus = "Not Started";
        }

        // Constructor with parameters
        public Project(String projectName, String projectDescription, String projectStatus,
                       List<DocumentReference> projectSubjects, Date projectDueDate, Date projectDueTime) {
            this.projectName = projectName;
            this.projectDescription = projectDescription;
            this.projectStatus = projectStatus;
            this.projectSubjects = projectSubjects;
            this.projectDueDate = projectDueDate;
            this.projectDueTime = projectDueTime;
        }

        // Getters and Setters
        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

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

        public List<DocumentReference> getProjectSubjects() {
            return projectSubjects;
        }

        public void setProjectSubjects(List<DocumentReference> subjects) {
            this.projectSubjects = subjects;
        }

        public Date getProjectDueDate() {
            return projectDueDate;
        }

        public void setProjectDueDate(Date projectDueDate) {
            this.projectDueDate = projectDueDate;
        }

        public Date getProjectDueTime() {
            return projectDueTime;
        }

        public void setProjectDueTime(Date projectDueTime) {
            this.projectDueTime = projectDueTime;
        }
    }
}
