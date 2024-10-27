package com.example.learnleaf;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.Map;
import java.util.function.BiConsumer;

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

        addNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateTaskDialog();
            }
        });
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

        // Check if any view is null
        if (subjectInput == null || projectInput == null || assignmentInput == null ||
                descriptionInput == null || prioritySpinner == null || statusSpinner == null) {
            Toast.makeText(this, "Error: Unable to create dialog", Toast.LENGTH_SHORT).show();
            return;
        }


        // Set up spinners
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);

        builder.setView(viewInflated);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String taskName = assignmentInput.getText().toString();
            String taskDescription = descriptionInput.getText().toString();
            String taskPriority = prioritySpinner.getSelectedItem().toString();
            String taskProjectId = projectInput.getText().toString();
            String taskSubjectId = subjectInput.getText().toString();

            createNewTask(taskName, taskDescription, taskPriority, taskProjectId, taskSubjectId, new OnTaskCreatedListener() {
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

    public void createNewTask(String taskName, String taskDescription, String taskProject,
                              String taskSubject, String taskPriority, OnTaskCreatedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("No user logged in");
            return;
        }

        Map<String, Object> newTask = new HashMap<>();
        newTask.put("taskName", taskName);
        newTask.put("taskDescription", taskDescription);
        newTask.put("taskPriority", taskPriority);
        newTask.put("taskStatus", "Not Started");

        // Convert project and subject to DocumentReferences
        DocumentReference projectRef = db.collection("projects").document(taskProject);
        DocumentReference subjectRef = db.collection("subjects").document(taskSubject);

        newTask.put("taskProject", projectRef);
        newTask.put("taskSubject", subjectRef);

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("tasks")
                .add(newTask)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure("Error creating task: " + e.getMessage()));
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
            noTasksText.setText("No tasks found");
            tasksContainer.addView(noTasksText);
            return;
        }

        for (Task task : tasks) {
            View taskView = getLayoutInflater().inflate(R.layout.task_item_block, null);

            TextView assignmentTextView = taskView.findViewById(R.id.taskNameTextView);
            assignmentTextView.setText(task.taskName);

            TextView descriptionTextView = taskView.findViewById(R.id.taskDescriptionTextView);
            descriptionTextView.setText(task.taskDescription);

            TextView priorityTextView = taskView.findViewById(R.id.taskPriorityTextView);
            priorityTextView.setText(task.taskPriority);

            TextView projectTextView = taskView.findViewById(R.id.taskProjectTextView);
            projectTextView.setText(task.taskProject != null ? task.taskProject.getId() : "No project");

            TextView statusTextView = taskView.findViewById(R.id.taskStatusTextView);
            statusTextView.setText(task.taskStatus);

            TextView subjectTextView = taskView.findViewById(R.id.taskSubjectTextView);
            subjectTextView.setText(task.taskSubject != null ? task.taskSubject.getId() : "No subject");

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

            if (task.getId() == null) {
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
            db.collection("users").document(userId).collection("tasks").document(task.getId())
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
        if (task.taskSubject != null) {
            task.taskSubject.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String subjectName = documentSnapshot.getString("name");
                    subjectInput.setText(subjectName != null ? subjectName : "");
                }
            }).addOnFailureListener(e -> {
                Log.e("EditTask", "Error fetching subject: ", e);
                subjectInput.setText("");
            });
        } else {
            subjectInput.setText("");
        }

        // Fetch and set project name
        if (task.taskProject != null) {
            task.taskProject.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String projectName = documentSnapshot.getString("name");
                    projectInput.setText(projectName != null ? projectName : "");
                }
            }).addOnFailureListener(e -> {
                Log.e("EditTask", "Error fetching project: ", e);
                projectInput.setText("");
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
            String newAssignment = assignmentInput.getText().toString();
            String newSubjectName = subjectInput.getText().toString();
            String newProjectName = projectInput.getText().toString();
            String newDescription = descriptionInput.getText().toString();
            String newPriority = prioritySpinner.getSelectedItem().toString();
            String newStatus = statusSpinner.getSelectedItem().toString();

            // Find or create subject and project references
            findOrCreateSubjectAndProject(newSubjectName, newProjectName, new SubjectProjectCallback() {
                @Override
                public void onReferencesReady(DocumentReference subjectRef, DocumentReference projectRef) {
                    updateTask(task, newAssignment, subjectRef, projectRef, newDescription, newPriority, newStatus);
                }
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

    private void updateTask(Task task, String newAssignment, DocumentReference newSubject, DocumentReference newProject,
                            String newDescription, String newPriority, String newStatus) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference taskRef = db.collection("users").document(userId).collection("tasks").document(task.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("taskName", newAssignment);
        updates.put("taskDescription", newDescription);
        updates.put("taskPriority", newPriority);
        updates.put("taskStatus", newStatus);
        updates.put("taskSubject", newSubject);
        updates.put("taskProject", newProject);

        taskRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Tasks.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                    fetchTasksForCurrentUser();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Tasks.this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    /*
    private void showDatePickerDialog(final TextView dateView) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year1, monthOfYear, dayOfMonth);
                    Date selectedDate = calendar.getTime();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String dateString = dateFormat.format(selectedDate);
                    dateView.setText(dateString);
                }, year, month, day);
        datePickerDialog.show();
    }

    private Date parseDateString(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateTask(Task task, String newAssignment, String newSubject, String newProject,
                            String newDescription, String newPriority, String newStatus) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Create a reference to the specific task document
        DocumentReference taskRef = db.collection("users").document(userId)
                .collection("tasks").document(task.id);

        Map<String, Object> updates = new HashMap<>();

        updates.put("taskName", newAssignment); // Update task name
        updates.put("taskDescription", newDescription); // Update task description
        updates.put("taskPriority", newPriority); // Update task priority
        updates.put("taskStatus", newStatus); // Update task status

        // Handle subject DocumentReference
        if (!newSubject.isEmpty()) {
            DocumentReference subjectRef = db.collection("subjects").document(newSubject);
            updates.put("taskSubject", subjectRef); // Update to the correct reference
        } else {
            updates.put("taskSubject", null); // Clear if empty
        }

        // Handle project DocumentReference
        if (!newProject.isEmpty()) {
            DocumentReference projectRef = db.collection("projects").document(newProject);
            updates.put("taskProject", projectRef); // Update to the correct reference
        } else {
            updates.put("taskProject", null); // Clear if empty
        }

        // Update the task in Firestore
        taskRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Tasks.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                    fetchTasksForCurrentUser(); // Refresh the task list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Tasks.this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

     */

    public static class Task {
        public String id; // Document ID
        public String userId; // User ID
        public String taskName; // Task name
        public String taskDescription; // Task description
        public String taskPriority; // Task priority
        public DocumentReference taskProject; // Project related to the task
        public String taskStatus; // Task status
        public DocumentReference taskSubject; // Subject related to the task

        // No-argument constructor
        public Task() {}

        public Task(String taskName, String taskDescription, String taskPriority,
                    DocumentReference taskProject, String taskStatus, DocumentReference taskSubject) {
            this.taskName = taskName;
            this.taskDescription = taskDescription;
            this.taskPriority = taskPriority;
            this.taskProject = taskProject;
            this.taskStatus = taskStatus;
            this.taskSubject = taskSubject;
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getTaskName() {
            return taskName;
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

