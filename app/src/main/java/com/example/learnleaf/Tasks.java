package com.example.learnleaf;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Tasks extends AppCompatActivity {
    private LinearLayout tasksContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Firebase firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasks);

        tasksContainer = findViewById(R.id.tasksContainer);
        if (tasksContainer == null) {
            throw new RuntimeException("Unable to find tasksContainer view. Check your layout file.");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebase = new Firebase(this);

        ImageView addNew = findViewById(R.id.addNewTask);
        if (addNew == null) {
            throw new RuntimeException("Unable to find addNewTask view. Check your layout file.");
        }
        fetchTasksForCurrentUser();

        addNew.setOnClickListener(v -> showCreateTaskDialog());
    }

    private void showCreateTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Task");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_task, null);

        // Find views
        EditText subjectInput = viewInflated.findViewById(R.id.taskSubjectTextView);
        EditText projectInput = viewInflated.findViewById(R.id.taskProjectTextView);
        EditText assignmentInput = viewInflated.findViewById(R.id.taskNameTextView);
        EditText descriptionInput = viewInflated.findViewById(R.id.taskDescriptionInput);
        Spinner prioritySpinner = viewInflated.findViewById(R.id.prioritySpinner);
        Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // New views for date and time
        Button startDateButton = viewInflated.findViewById(R.id.startDateButton);
        Button dueDateButton = viewInflated.findViewById(R.id.dueDateButton);
        Button dueTimeButton = viewInflated.findViewById(R.id.dueTimeButton);

        // Check if any view is null
        if (subjectInput == null || projectInput == null || assignmentInput == null ||
                descriptionInput == null || prioritySpinner == null || statusSpinner == null ||
                startDateButton == null || dueDateButton == null || dueTimeButton == null) {
            Toast.makeText(this, "Error: Unable to create dialog", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set up spinners (unchanged)
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        // Variables to store date and time
        final Calendar calendar = Calendar.getInstance();
        final Date[] startDate = {null};
        final Date[] dueDate = {null};
        final Date[] dueTime = {null};

        // Set up date pickers (unchanged)
        DatePickerDialog.OnDateSetListener startDateListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            startDate[0] = calendar.getTime();
            startDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate[0]));
        };

        DatePickerDialog.OnDateSetListener dueDateListener = (view, year, month, day) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            dueDate[0] = calendar.getTime();
            dueDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dueDate[0]));
        };

        // Set up time picker
        TimePickerDialog.OnTimeSetListener dueTimeListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            dueTime[0] = calendar.getTime();
            dueTimeButton.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueTime[0]));
        };

        // Set click listeners for date and time buttons
        startDateButton.setOnClickListener(v -> new DatePickerDialog(Tasks.this, startDateListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show());

        dueDateButton.setOnClickListener(v -> new DatePickerDialog(Tasks.this, dueDateListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show());

        dueTimeButton.setOnClickListener(v -> new TimePickerDialog(Tasks.this, dueTimeListener,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show());

        builder.setView(viewInflated);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String taskName = assignmentInput.getText().toString();
            String taskDescription = descriptionInput.getText().toString();
            String taskPriority = prioritySpinner.getSelectedItem().toString();
            String taskProject = projectInput.getText().toString();
            String taskSubject = subjectInput.getText().toString();
            String taskStatus = statusSpinner.getSelectedItem().toString();

            // Use the static ArrayList from Firebase class
            createNewTask(taskName, taskDescription, taskProject, taskSubject, taskPriority, taskStatus,
                    startDate[0], dueDate[0], dueTime[0], Firebase.localProjects, Firebase.localSubjects,
                    new OnTaskCreatedListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(Tasks.this, "Task created successfully", Toast.LENGTH_SHORT).show();
                            fetchTasksForCurrentUser();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(Tasks.this, "Error creating task: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Implement these methods to fetch your local projects and subjects
    private List<Projects.Project> getLocalProjects() {
        // Check if the list is empty or null, and fetch if necessary
        if (Firebase.localProjects == null || Firebase.localProjects.isEmpty()) {
            fetchProjects();
        }
        return Firebase.localProjects;
    }

    private List<Subjects.Subject> getActiveSubjects() {
        // Check if the list is empty or null, and fetch if necessary
        if (Firebase.localSubjects == null || Firebase.localSubjects.isEmpty()) {
            fetchActiveSubjects();
        }
        return Firebase.localSubjects;
    }

    // Helper method to fetch projects
    private void fetchProjects() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        firebase.fetchProjectsForCurrentUser(new Firebase.OnProjectsFetchedListener() {
            @Override
            public void onSuccess(List<Projects.Project> projects) {
                // Projects are now in Firebase.localProjects
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("ProjectFetch", "Failed to fetch projects: " + errorMessage);
                // Handle the error, maybe show a toast to the user
            }
        });
    }

    // Helper method to fetch active subjects
    private void fetchActiveSubjects() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        firebase.fetchActiveSubjectsForCurrentUser(new Firebase.OnActiveSubjectsFetchedListener() {
            @Override
            public void onSuccess(List<Subjects.Subject> subjects) {
                // Active subjects are now in Firebase.activeSubjectsLocal
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("SubjectFetch", "Failed to fetch active subjects: " + errorMessage);
                // Handle the error, maybe show a toast to the user
            }
        });
    }

    public void createNewTask(String taskName, String taskDescription, String taskProject,
                              String taskSubject, String taskPriority, String taskStatus,
                              Date startDate, Date dueDate, Date dueTime,
                              List<Projects.Project> localProjects, List<Subjects.Subject> activeSubjects,
                              OnTaskCreatedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("No user logged in");
            return;
        }

        Map<String, Object> newTask = new HashMap<>();
        newTask.put("taskName", taskName);
        newTask.put("taskDescription", taskDescription);
        newTask.put("taskPriority", taskPriority);
        newTask.put("taskStatus", taskStatus);

        // Convert project and subject to DocumentReferences
        DocumentReference projectRef = db.collection("projects").document(taskProject);
        DocumentReference subjectRef = db.collection("subjects").document(taskSubject);

        // Find and add project name
        String projectName = findProjectName(taskProject, localProjects);
        newTask.put("taskProject", projectRef);
        newTask.put("taskProjectName", projectName);

        // Find and add subject name
        String subjectName = findSubjectName(taskSubject, activeSubjects);
        newTask.put("taskSubject", subjectRef);
        newTask.put("taskSubjectName", subjectName);

        // Add new attributes
        if (startDate != null) {
            newTask.put("taskStartDate", startDate);
        }
        if (dueDate != null) {
            newTask.put("taskDueDate", dueDate);
        }
        if (dueTime != null) {
            // Convert Time to Timestamp for Firestore
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dueTime);
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            newTask.put("taskDueTime", new Timestamp(hours, minutes));
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("tasks")
                .add(newTask)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure("Error creating task: " + e.getMessage()));
    }

    private String findProjectName(String projectId, List<Projects.Project> localProjects) {
        for (Projects.Project project : localProjects) {
            if (project.getProjectName().equals(projectId)) {
                return project.getProjectName();
            }
        }
        return "Unknown Project";
    }

    private String findSubjectName(String subjectId, List<Subjects.Subject> activeSubjects) {
        for (Subjects.Subject subject : activeSubjects) {
            if (subject.getSubjectName().equals(subjectId)) {
                return subject.getSubjectName();
            }
        }
        return "Unknown Subject";
    }

    public interface OnTaskCreatedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }



    private void fetchTasksForCurrentUser() {
        //showLoadingIndicator();
        firebase.fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                updateUI(tasks);
                //hideLoadingIndicator();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("Tasks", "Failed to fetch tasks: " + errorMessage);
                Toast.makeText(Tasks.this, errorMessage, Toast.LENGTH_SHORT).show();
                updateUI(new ArrayList<>()); // Update UI with empty list
                //hideLoadingIndicator();
            }
        });
    }


    private void updateUI(List<Task> tasks) {
        tasksContainer.removeAllViews(); // Clear existing views

        if (tasks.isEmpty()) {
            TextView noTasksText = new TextView(this);
            noTasksText.setText(R.string.no_tasks_found);
            tasksContainer.addView(noTasksText);
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        for (Task task : tasks) {
            @SuppressLint("InflateParams") View taskView = getLayoutInflater().inflate(R.layout.task_item_block, null);

            TextView assignmentTextView = taskView.findViewById(R.id.taskNameTextView);
            assignmentTextView.setText(task.taskName);

            TextView descriptionTextView = taskView.findViewById(R.id.taskDescriptionTextView);
            descriptionTextView.setText(task.taskDescription);

            TextView priorityTextView = taskView.findViewById(R.id.taskPriorityTextView);
            priorityTextView.setText(task.taskPriority);

            TextView projectTextView = taskView.findViewById(R.id.taskProjectTextView);
            projectTextView.setText(task.taskProject != null ? task.taskProject : "No project");

            TextView statusTextView = taskView.findViewById(R.id.taskStatusTextView);
            statusTextView.setText(task.taskStatus);

            TextView subjectTextView = taskView.findViewById(R.id.taskSubjectTextView);
            subjectTextView.setText(task.taskSubject != null ? task.taskSubject : "No subject");

            // Set Start Date
            TextView startDateTextView = taskView.findViewById(R.id.startDateTextView);
            if (task.taskStartDate != null) {
                startDateTextView.setText("Start: " + dateFormat.format(task.taskStartDate));
            } else {
                startDateTextView.setText("Start: Not set");
            }

            // Set Due Date
            TextView dueDateTextView = taskView.findViewById(R.id.dueDateTextView);
            if (task.taskDueDate != null) {
                dueDateTextView.setText("Due: " + dateFormat.format(task.taskDueDate));
            } else {
                dueDateTextView.setText("Due: Not set");
            }

            // Set Due Time
            TextView dueTimeTextView = taskView.findViewById(R.id.dueTimeTextView);
            if (task.getTaskDueTime() != null) {
                dueTimeTextView.setText("Time: " + timeFormat.format(task.getTaskDueTime()));
            } else {
                dueTimeTextView.setText("Time: Not set");
            }

            ImageButton editButton = taskView.findViewById(R.id.editButton);
            editButton.setOnClickListener(v -> showEditTaskDialog(task));

            ImageButton deleteButton = taskView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(v -> deleteTask(task, taskView));

            tasksContainer.addView(taskView);
        }
    }

    private void deleteTask(Task task, View taskView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task?");
        builder.setPositiveButton("Yes", (dialog, which) -> {

            if (task.getTaskName() == null) {
                Toast.makeText(this, "Error: Task ID is null", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).collection("tasks").document(task.getTaskName())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        tasksContainer.removeView(taskView);
                        Toast.makeText(Tasks.this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(Tasks.this, "Error deleting task", Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Task");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);
        final EditText assignmentInput = viewInflated.findViewById(R.id.assignmentInput);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);
        final EditText projectInput = viewInflated.findViewById(R.id.projectInput);
        final EditText descriptionInput = viewInflated.findViewById(R.id.descriptionInput);
        final Spinner prioritySpinner = viewInflated.findViewById(R.id.prioritySpinner);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // Pre-fill the fields with current task data
        assignmentInput.setText(task.taskName);
        descriptionInput.setText(task.taskDescription);

        // Fetch and set subject name
        if (task.subjectRef != null) {
            task.subjectRef.get().addOnSuccessListener((OnSuccessListener<DocumentSnapshot>) documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String subjectName = documentSnapshot.get("name", String.class);
                    subjectInput.setText(subjectName != null ? subjectName : "");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("EditTask", "Error fetching subject: ", e);
                    subjectInput.setText("");
                }
            });
        } else {
            subjectInput.setText("");
        }

// Fetch and set project name
        if (task.projectRef != null) {
            task.projectRef.get().addOnSuccessListener((OnSuccessListener<DocumentSnapshot>) documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String projectName = documentSnapshot.get("name", String.class);
                    projectInput.setText(projectName != null ? projectName : "");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("EditTask", "Error fetching project: ", e);
                    projectInput.setText("");
                }
            });
        } else {
            projectInput.setText("");
        }

        // Set up spinners
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(priorityAdapter.getPosition(task.taskPriority));

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setSelection(statusAdapter.getPosition(task.taskStatus));

        builder.setView(viewInflated);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newSubjectName = subjectInput.getText().toString();
            String newProjectName = projectInput.getText().toString();

            // Find or create subject and project references
            findOrCreateSubjectAndProject(newSubjectName, newProjectName, (subjectRef, projectRef) -> {

            });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void findOrCreateSubjectAndProject(String subjectName, String projectName, SubjectProjectCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Find or create subject
        db.collection("users").document(userId).collection("subjects")
                .whereEqualTo("name", subjectName)
                .get()
                .addOnSuccessListener(subjectQuerySnapshot -> {
                    DocumentReference subjectRef;
                    if (subjectQuerySnapshot.isEmpty()) {
                        // Create new subject
                        Map<String, Object> subjectData = new HashMap<>();
                        subjectData.put("name", subjectName);
                        subjectRef = db.collection("users").document(userId).collection("subjects").document();
                        subjectRef.set(subjectData);
                    } else {
                        subjectRef = subjectQuerySnapshot.getDocuments().get(0).getReference();
                    }

                    // Find or create project
                    db.collection("users").document(userId).collection("projects")
                            .whereEqualTo("name", projectName)
                            .get()
                            .addOnSuccessListener(projectQuerySnapshot -> {
                                DocumentReference projectRef;
                                if (projectQuerySnapshot.isEmpty()) {
                                    // Create new project
                                    Map<String, Object> projectData = new HashMap<>();
                                    projectData.put("name", projectName);
                                    projectRef = db.collection("users").document(userId).collection("projects").document();
                                    projectRef.set(projectData);
                                } else {
                                    projectRef = projectQuerySnapshot.getDocuments().get(0).getReference();
                                }

                                // Call the callback with both references
                                callback.onReferencesReady(subjectRef, projectRef);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EditTask", "Error finding/creating project", e);
                                Toast.makeText(Tasks.this, "Error updating task", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("EditTask", "Error finding/creating subject", e);
                    Toast.makeText(Tasks.this, "Error updating task", Toast.LENGTH_SHORT).show();
                });
    }

    private interface SubjectProjectCallback {
        void onReferencesReady(DocumentReference subjectRef, DocumentReference projectRef);

    }


    public static class Task {
        public String taskName; // Task name
        public String taskDescription; // Task description
        public String taskPriority; // Task priority
        public DocumentReference projectRef;
        private String taskProject;
        public DocumentReference subjectRef;
        private String taskSubject; // Project related to the task
        public String taskStatus; // Task status
        public Date taskDueDate;
        public Date taskDueTime;
        public Date taskStartDate;


        // No-argument constructor
        public Task() {}

        public Task(String taskName, String taskDescription, String taskPriority,
                    DocumentReference taskProject, String taskStatus,
                    DocumentReference taskSubject, Date taskDueDate, Date taskStartDate,Date taskDueTime) {
            this.taskName = taskName;
            this.taskDescription = taskDescription;
            this.taskPriority = taskPriority;
            this.projectRef = taskProject;
            this.taskStatus = taskStatus;
            this.subjectRef = taskSubject;
            this.taskDueDate = taskDueDate;
            this.taskStartDate = taskStartDate;
            this.taskDueTime = taskDueTime;

        }

        // Getters and setters

        public String getTaskName() {
            return taskName;
        }

        public Date getTaskDueTime() {
            return taskDueTime;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public void setTaskDescription(String taskDescription) {
            this.taskDescription = taskDescription;
        }

        public String getTaskPriority() {
            return taskPriority;
        }

        public void setTaskPriority(String taskPriority) {
            this.taskPriority = taskPriority;
        }

        public String getTaskStatus() {
            return taskStatus;
        }

        public void setTaskStatus(String taskStatus) {
            this.taskStatus = taskStatus;
        }

    }

}

