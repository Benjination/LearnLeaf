package com.example.learnleaf;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

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
import java.util.Objects;
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
        ImageView addNew = findViewById(R.id.addnew);

        addNew.setOnClickListener(v -> showCreateProjectDialog());

        //Creates list of Projects to be displayed in item_blocks in Scrollview
        firebase.fetchProjectsForCurrentUser(new Firebase.OnProjectsFetchedListener() {
            @Override
            public void onSuccess(List<Projects.Project> projects) {
                // Update your UI with the new list of projects
                updateUI(projects);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Projects.this, "Failed to fetch projects: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //This is part of the function of the addNew button -> Calls dialog_create_project to collect
    //project information from the user, and adds new Project to Firebase and localDatabase using
    //Firebase.java function "createNewProject()"
    private void showCreateProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Project");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = currentUser.getUid();

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_project, null);
        final EditText projectNameInput = viewInflated.findViewById(R.id.projectNameInput);
        final EditText projectDescriptionInput = viewInflated.findViewById(R.id.projectDescriptionInput);
        final Button projectDueDateButton = viewInflated.findViewById(R.id.dueDateButton);
        final Button projectDueTimeButton = viewInflated.findViewById(R.id.dueTimeButton);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);

        // Sets up the status spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);

        // Initialize date and time
        final Calendar calendar = Calendar.getInstance();
        final Date[] projectDueDate = {null};
        final Date[] projectDueTime = {null};

        //SubMenu interface that allows user to select dates instead of entering them
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

        //Submenu interface that allows user to select time from 24 hour clock rather than entering a specific time
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

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false); // Initially disable the button

            projectNameInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    positiveButton.setEnabled(!s.toString().trim().isEmpty());
                }
            });

            positiveButton.setOnClickListener(v -> {
                String projectName = projectNameInput.getText().toString().trim();
                String projectDescription = projectDescriptionInput.getText().toString();
                String status = statusSpinner.getSelectedItem().toString();

                String subjectString = subjectInput.getText().toString();
                List<String> projectSubjects = Arrays.asList(subjectString.split(",\\s*"));

                // Converts subject strings to DocumentReferences
                List<DocumentReference> subjectRefs = new ArrayList<>();
                for (String subject : projectSubjects) {
                    subjectRefs.add(db.collection("users").document(userId).collection("subjects").document(subject));
                }

                createNewProject(projectName, projectDescription, status, subjectRefs, projectDueDate[0], projectDueTime[0]);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    //Calls the createNewProject function in Firebase
    //Calls require changing the attributes, and I wanted to include toast messages
    //These could have been done on each function call, but this is a more organized approach that is easier to understand
    //Most of my calls to firebase will be in this fashion
    private void createNewProject(String projectName, String projectDescription, String projectStatus,
                                  List<DocumentReference> projectSubjects, Date projectDueDate, Date projectDueTime) {
        firebase.createNewProject(projectName, projectDescription, projectStatus, projectSubjects,projectDueDate,
                projectDueTime, new Firebase.OnProjectCreatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Projects.this, "Project created successfully", Toast.LENGTH_SHORT).show();
                firebase.fetchProjectsForCurrentUser(new Firebase.OnProjectsFetchedListener() {
                    @Override
                    public void onSuccess(List<Projects.Project> projects) {
                        // Update your UI with the new list of projects
                        updateUI(projects);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(Projects.this, "Failed to fetch projects: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Projects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //This opens a subMenu called dialog_edit_project to allow the user to edit an existing project
    private void showEditProjectDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Project");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_project, null);
        final EditText projectNameInput = viewInflated.findViewById(R.id.projectNameInput);
        final EditText projectDescriptionInput = viewInflated.findViewById(R.id.projectDescriptionInput);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        //Pre-fills the input fields with current project data
        projectNameInput.setText(project.getProjectName());
        projectDescriptionInput.setText(project.getProjectDescription());

        //Sets up the status spinner
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

            List<String> projectSubjects = new ArrayList<>();

            updateProject(project.getProjectName(), newProjectName, projectDescription, status, projectSubjects);// Updated call
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    //Finds Project on Firebase and prepares new data to replace old data
    public void updateProject(String oldProjectName, String newProjectName, String projectDescription, String projectStatus, List<String> subjectNames) {

        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        //Finds Project by Old Name
        db.collection("users").document(userId).collection("projects")
                .whereEqualTo("projectName", oldProjectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot projectDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String projectId = projectDoc.getId();

                        //Prepare the updates
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("projectName", newProjectName);
                        updates.put("projectDescription", projectDescription);
                        updates.put("projectStatus", projectStatus);

                        //Special case to prepare Project.Subjects if they exist
                        if (!subjectNames.isEmpty()) {
                            List<DocumentReference> subjectRefs = new ArrayList<>();
                            for (String subjectName : subjectNames) {
                                db.collection("users").document(userId).collection("subjects")
                                        .whereEqualTo("subjectName", subjectName)
                                        .get()
                                        .addOnSuccessListener(subjectQuerySnapshot -> {
                                            if (!subjectQuerySnapshot.isEmpty()) {
                                                DocumentSnapshot subjectDoc = subjectQuerySnapshot.getDocuments().get(0);
                                                subjectRefs.add(subjectDoc.getReference());

                                                if (subjectRefs.size() == subjectNames.size()) {
                                                    updates.put("projectSubjects", subjectRefs);
                                                    updateProjectInFirestore(userId, projectId, updates);
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error finding subject: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            //New subjects do not exist
                            updateProjectInFirestore(userId, projectId, updates);
                        }
                    } else {
                        Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error finding project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    //Sends new data to Firebase after edit
    private void updateProjectInFirestore(String userId, String projectId, Map<String, Object> updates) {
        db.collection("users").document(userId).collection("projects").document(projectId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();
                    //updates local database
                    firebase.fetchProjectsForCurrentUser(new Firebase.OnProjectsFetchedListener() {
                        @Override
                        public void onSuccess(List<Projects.Project> projects) {
                            updateUI(projects); //Call to display updated project list
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(Projects.this, "Failed to refresh projects: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error updating project: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    //Calls method in Firebase and Toasts to your health
    private void deleteProject(String projectName, View blockView) {
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

    //Double checks with user to make they want to delete a project
    private void showDeleteConfirmationDialog(Project project, View blockView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete this project?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteProject(project.getProjectName(), blockView))
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    //passes most recent collection of projects to item_blocks for Scrollview
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

            nameTextView.setText(project.getProjectName());
            statusTextView.setText("Status: " + project.getProjectStatus());

            Date dueDate = project.getProjectDueDate();
            Date dueTime = project.getProjectDueTime();

            //opens dialog_edit_projects
            editButton.setOnClickListener(v -> {
                Log.d("EditButton", "Edit button clicked");
                showEditProjectDialog(project);
            });

            //deletes project associated with item_block that contains a project
            deleteButton.setOnClickListener(v -> {
                Log.d("DeleteButton", "Delete button clicked");
                showDeleteConfirmationDialog(project, blockView);
            });

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

            //Special case for subjects referenced in projects
            List<DocumentReference> subjectRefs = project.getProjectSubjects();

            if (subjectRefs != null && !subjectRefs.isEmpty()) {
                List<String> subjectNames = new ArrayList<>();
                Task<Void> fetchSubjectsTask = null;
                //SDK constraint version 34+ requirement
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    fetchSubjectsTask = Tasks.whenAllComplete(
                            subjectRefs.stream()
                                    .map(ref -> ref.get().continueWith(task -> {
                                        if (task.isSuccessful() && task.getResult() != null) {
                                            DocumentSnapshot subjectDoc = task.getResult();
                                            String subjectName = subjectDoc.getString("subjectName");
                                            if (subjectName != null) {
                                                subjectNames.add(subjectName);
                                            }
                                        }
                                        return null;
                                    }))
                                    .collect(Collectors.toList())
                    ).continueWith(task -> {
                        String subjectsString = String.join(", ", subjectNames);
                        extraTextView.setText("Subjects: " + subjectsString);

                        return null;
                    });
                }

                //Updates UI after fetching all subjects
                fetchSubjectsTask.addOnSuccessListener(aVoid -> {
                    String contentDescription = String.format("Project: %s, Status: %s, Subjects: %s, Due Date: %s, Due Time: %s",
                            project.getProjectName(), project.getProjectStatus(), String.join(", ", subjectNames),
                            dueDate != null ? dateFormat.format(dueDate) : "Not set",
                            dueTime != null ? timeFormat.format(dueTime) : "Not set");
                    cardView.setContentDescription(contentDescription);

                    //adds block to Scrollview
                    projectsContainer.addView(blockView);
                });

                fetchSubjectsTask.addOnFailureListener(e -> {
                    extraTextView.setText("Subjects: Error fetching subjects");
                    projectsContainer.addView(blockView);
                });

            } else {
                extraTextView.setText("Subjects: None");
                projectsContainer.addView(blockView);
            }
        }
    }


    //Object definition for Project
    public static class Project {
        private String projectId;
        private String projectName;
        private String projectDescription;
        private String projectStatus;
        private List<DocumentReference> projectSubjects;
        private Date projectDueDate;
        private Date projectDueTime;

        //No attribute constructor Required for Firebase
        //Do not Delete
        public Project() {}

        public Project(String projectName, String projectDescription, String projectStatus,
                       List<DocumentReference> projectSubjects, Date projectDueDate, Date projectDueTime) {
            this.projectName = projectName;
            this.projectDescription = projectDescription;
            this.projectStatus = projectStatus;
            this.projectSubjects = projectSubjects;
            this.projectDueDate = projectDueDate;
            this.projectDueTime = projectDueTime;
        }

        //Setters
        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        //Getters
        public String getProjectName() {
            return projectName;
        }
        public String getProjectDescription() {
            return projectDescription;
        }
        public String getProjectStatus() {
            return projectStatus;
        }
        public List<DocumentReference> getProjectSubjects() {
            return projectSubjects;
        }
        public Date getProjectDueDate() {
            return projectDueDate;
        }
        public Date getProjectDueTime() {
            return projectDueTime;
        }
    }
}
