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
import com.google.firebase.firestore.FieldValue;
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

        fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
            @Override
            public void onSuccess(List<Tasks.Task> tasks) {
                updateUI(tasks);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Tasks.this, "Failed to fetch tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("FetchTasks", "Error fetching tasks: " + errorMessage);
            }
        });

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

        // Set up spinners
        setupSpinners(prioritySpinner, statusSpinner);

        // Variables to store date and time
        final Calendar calendar = Calendar.getInstance();
        final Date[] startDate = {null};
        final Date[] dueDate = {null};
        final Date[] dueTime = {null};

        // Set up date and time pickers
        setupDateTimePickers(startDateButton, dueDateButton, dueTimeButton, calendar, startDate, dueDate, dueTime);

        builder.setView(viewInflated);



        builder.setPositiveButton("Create", (dialog, which) -> {
            String taskName = assignmentInput.getText().toString();
            String taskDescription = descriptionInput.getText().toString();
            String taskPriority = prioritySpinner.getSelectedItem().toString();
            String taskProject = projectInput.getText().toString();
            String taskSubject = subjectInput.getText().toString();
            String taskStatus = statusSpinner.getSelectedItem().toString();

            List<Projects.Project> localProjects = getLocalProjects();
            List<Subjects.Subject> activeSubjects = getActiveSubjects();

            createNewTask(taskName, taskDescription, taskProject, taskSubject, taskPriority, taskStatus,
                    startDate[0], dueDate[0], dueTime[0], localProjects, activeSubjects,
                    new OnTaskCreatedListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(Tasks.this, "Task created successfully", Toast.LENGTH_SHORT).show();
                            // Fetch and update the tasks list
                            fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
                                @Override
                                public void onSuccess(List<Tasks.Task> tasks) {
                                    updateUI(tasks); // Update the UI with the new list of tasks
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    Toast.makeText(Tasks.this, "Failed to fetch tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(Tasks.this, "Failed to create task: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });


        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupSpinners(Spinner prioritySpinner, Spinner statusSpinner) {
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
    }

    private void setupDateTimePickers(Button startDateButton, Button dueDateButton, Button dueTimeButton,
                                      Calendar calendar, Date[] startDate, Date[] dueDate, Date[] dueTime) {
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

        TimePickerDialog.OnTimeSetListener dueTimeListener = (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            dueTime[0] = calendar.getTime();
            dueTimeButton.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueTime[0]));
        };

        startDateButton.setOnClickListener(v -> new DatePickerDialog(Tasks.this, startDateListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show());

        dueDateButton.setOnClickListener(v -> new DatePickerDialog(Tasks.this, dueDateListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show());

        dueTimeButton.setOnClickListener(v -> new TimePickerDialog(Tasks.this, dueTimeListener,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show());
    }

    private List<Projects.Project> getLocalProjects() {
        if (Firebase.localProjects == null || Firebase.localProjects.isEmpty()) {
            fetchProjects();
        }
        return Firebase.localProjects;
    }

    private List<Subjects.Subject> getActiveSubjects() {
        if (Firebase.localSubjects == null || Firebase.localSubjects.isEmpty()) {
            fetchActiveSubjects();
        }
        return Firebase.localSubjects;
    }

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
            }
        });
    }

    private void fetchActiveSubjects() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        firebase.fetchAllSubjectsForCurrentUser(new Firebase.OnAllSubjectsFetchedListener() {
            @Override
            public void onSuccess(List<Subjects.Subject> subjects) {
                // Subjects are now in Firebase.localSubjects
                // No need to update UI here, as this is just fetching data
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("SubjectFetch", "Failed to fetch subjects: " + errorMessage);
                runOnUiThread(() -> Toast.makeText(Tasks.this, "Failed to fetch subjects: " + errorMessage, Toast.LENGTH_SHORT).show());
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

        String userId = currentUser.getUid();

        findOrCreateSubjectAndProject(taskSubject, taskProject, dueDate, dueTime, new OnSubjectAndProjectEnsuredListener() {
            @Override
            public void onReferencesReady(DocumentReference subjectRef, DocumentReference projectRef) {
                // Ensure this block is only executed once
                if (subjectRef != null && projectRef != null) {
                    Map<String, Object> newTask = new HashMap<>();
                    newTask.put("taskDescription", taskDescription);
                    newTask.put("taskDueDate", dueDate);
                    newTask.put("taskDueTime", dueTime);
                    newTask.put("taskName", taskName);
                    newTask.put("taskPriority", taskPriority);
                    newTask.put("taskProject", projectRef);
                    newTask.put("taskStartDate", startDate);
                    newTask.put("taskStatus", taskStatus);
                    newTask.put("taskSubject", subjectRef); // Ensure this is set correctly


                    if (startDate != null) {
                        newTask.put("taskStartDate", startDate);
                    }
                    if (dueDate != null) {
                        newTask.put("taskDueDate", dueDate);
                    }
                    if (dueTime != null) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(dueTime);
                        int hours = calendar.get(Calendar.HOUR_OF_DAY);
                        int minutes = calendar.get(Calendar.MINUTE);
                        newTask.put("taskDueTime", new Timestamp(hours, minutes));
                    }

                    // Add the new task to Firestore
                    db.collection("users").document(userId).collection("tasks")
                            .add(newTask)
                            .addOnSuccessListener(documentReference -> {
                                listener.onSuccess();
                                fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
                                    @Override
                                    public void onSuccess(List<Tasks.Task> tasks) {
                                        if (listener instanceof OnTasksUpdatedListener) {
                                            ((OnTasksUpdatedListener) listener).onTasksUpdated(tasks);
                                        }
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Log.e("TaskFetch", "Failed to fetch tasks after creation: " + errorMessage);
                                    }
                                });
                            })
                            .addOnFailureListener(e -> listener.onFailure("Error creating task: " + e.getMessage()));
                } else {
                    listener.onFailure("Subject or Project references are invalid.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                listener.onFailure("Error ensuring subject and project: " + errorMessage);
            }
        });
    }



    public interface OnTasksUpdatedListener extends OnTaskCreatedListener {
        void onTasksUpdated(List<Tasks.Task> tasks);
    }


    public interface OnTaskCreatedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnSubjectAndProjectEnsuredListener {
        void onReferencesReady(DocumentReference subjectRef, DocumentReference projectRef);
        void onFailure(String errorMessage);
    }



    private void updateUI(List<Tasks.Task> tasks) {
        if (tasksContainer == null) {
            Toast.makeText(Tasks.this, "Null container", Toast.LENGTH_SHORT).show();
            return;
        }

        tasksContainer.removeAllViews();

        if (tasks.isEmpty()) {
            TextView noTasksText = new TextView(this);
            noTasksText.setText("No tasks found");
            tasksContainer.addView(noTasksText);
            return;
        }

        for (Tasks.Task task : tasks) {
            View taskView = getLayoutInflater().inflate(R.layout.task_item_block, null);

            TextView assignmentTextView = taskView.findViewById(R.id.taskNameTextView);
            assignmentTextView.setText(task.taskName);

            TextView descriptionTextView = taskView.findViewById(R.id.taskDescriptionTextView);
            descriptionTextView.setText(task.taskDescription);

            TextView priorityTextView = taskView.findViewById(R.id.taskPriorityTextView);
            priorityTextView.setText(task.taskPriority);

            TextView projectTextView = taskView.findViewById(R.id.taskProjectTextView);
            TextView subjectTextView = taskView.findViewById(R.id.taskSubjectTextView);

            // Fetch project name
            if (task.taskProject != null) {
                task.taskProject.get().addOnSuccessListener(documentSnapshot -> {
                    String projectName = documentSnapshot.getString("projectName");
                    projectTextView.setText(projectName != null ? projectName : "Unknown Project");
                }).addOnFailureListener(e -> {
                    projectTextView.setText("Error fetching project");
                    Log.e("UpdateUI", "Error fetching project", e);
                });
            } else {
                projectTextView.setText("No Project");
            }

            // Fetch subject name
            if (task.taskSubject != null) {
                task.taskSubject.get().addOnSuccessListener(documentSnapshot -> {
                    String subjectName = documentSnapshot.getString("subjectName");
                    subjectTextView.setText(subjectName != null ? subjectName : "Unknown Subject");
                }).addOnFailureListener(e -> {
                    subjectTextView.setText("Error fetching subject");
                    Log.e("UpdateUI", "Error fetching subject", e);
                });
            } else {
                subjectTextView.setText("No Subject");
            }

            TextView statusTextView = taskView.findViewById(R.id.taskStatusTextView);
            statusTextView.setText(task.taskStatus);

            // Set Start Date
            TextView startDateTextView = taskView.findViewById(R.id.startDateTextView);
            if (task.taskStartDate != null) {
                startDateTextView.setText("Start: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.taskStartDate));
            } else {
                startDateTextView.setText("Start: Not set");
            }

            // Set Due Date
            TextView dueDateTextView = taskView.findViewById(R.id.dueDateTextView);
            if (task.taskDueDate != null) {
                dueDateTextView.setText("Due: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.taskDueDate));
            } else {
                dueDateTextView.setText("Due: Not set");
            }

            // Set Due Time
            TextView dueTimeTextView = taskView.findViewById(R.id.dueTimeTextView);
            if (task.taskDueTime != null) {
                dueTimeTextView.setText("Time: " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(task.taskDueTime));
            } else {
                dueTimeTextView.setText("Time: Not set");
            }

            ImageButton editButton = taskView.findViewById(R.id.editButton);
            editButton.setOnClickListener(v -> showEditTaskDialog(task));

            ImageButton deleteButton = taskView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(v -> deleteTask(task));

            tasksContainer.addView(taskView);
        }
    }

    private void deleteTask(Tasks.Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (task.taskId == null) {
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
            db.collection("users").document(userId).collection("tasks").document(task.taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Tasks.this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                        // Fetch and update the task list after successful deletion
                        fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
                            @Override
                            public void onSuccess(List<Tasks.Task> tasks) {
                                updateUI(tasks); // Update the UI with the new list of tasks
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(Tasks.this, "Failed to fetch tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Tasks.this, "Error deleting task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("DeleteTask", "Error deleting task", e);
                    });
        });


        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Task");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);
        // Find views
        final EditText assignmentInput = viewInflated.findViewById(R.id.assignmentInput);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);
        final EditText projectInput = viewInflated.findViewById(R.id.projectInput);
        final EditText descriptionInput = viewInflated.findViewById(R.id.descriptionInput);
        final Spinner prioritySpinner = viewInflated.findViewById(R.id.prioritySpinner);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // New views for date and time
        Button startDateButton = viewInflated.findViewById(R.id.startDateButton);
        Button dueDateButton = viewInflated.findViewById(R.id.dueDateButton);
        Button dueTimeButton = viewInflated.findViewById(R.id.dueTimeButton);

        // Pre-fill the fields with current task data
        assignmentInput.setText(task.taskName);
        descriptionInput.setText(task.taskDescription);

        // Fetch and set subject name
        if (task.taskSubject != null) {
            task.taskSubject.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String subjectName = documentSnapshot.getString("subjectName");
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
                    String projectName = documentSnapshot.getString("projectName");
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

        // Set up date and time variables
        final Calendar calendar = Calendar.getInstance();
        final Date[] startDate = {task.taskStartDate};
        final Date[] dueDate = {task.taskDueDate};
        final Date[] dueTime = {task.taskDueTime};

        // Set button texts with current dates and times if available
        if (startDate[0] != null) {
            startDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate[0]));
        }
        if (dueDate[0] != null) {
            dueDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dueDate[0]));
        }
        if (dueTime[0] != null) {
            dueTimeButton.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueTime[0]));
        }

        // Set up date and time pickers
        startDateButton.setOnClickListener(v -> {
            new DatePickerDialog(Tasks.this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                startDate[0] = calendar.getTime();
                startDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate[0]));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        dueDateButton.setOnClickListener(v -> {
            new DatePickerDialog(Tasks.this, (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                dueDate[0] = calendar.getTime();
                dueDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dueDate[0]));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        dueTimeButton.setOnClickListener(v -> {
            new TimePickerDialog(Tasks.this, (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                dueTime[0] = calendar.getTime();
                dueTimeButton.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueTime[0]));
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        });

        builder.setView(viewInflated);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newSubjectName = subjectInput.getText().toString();
            String newProjectName = projectInput.getText().toString();

            // Find or create subject and project references
            findOrCreateSubjectAndProject(newSubjectName, newProjectName, dueDate[0], dueTime[0], new OnSubjectAndProjectEnsuredListener() {
                @Override
                public void onReferencesReady(DocumentReference subjectRef, DocumentReference projectRef) {
                    // Prepare the updated task data
                    Map<String, Object> updatedTask = new HashMap<>();
                    updatedTask.put("taskName", assignmentInput.getText().toString());
                    updatedTask.put("taskDescription", descriptionInput.getText().toString());
                    updatedTask.put("taskPriority", prioritySpinner.getSelectedItem().toString());
                    updatedTask.put("taskStatus", statusSpinner.getSelectedItem().toString());
                    updatedTask.put("taskProject", projectRef);
                    updatedTask.put("taskSubject", subjectRef);

                    if (startDate[0] != null) {
                        updatedTask.put("taskStartDate", startDate[0]);
                    }
                    if (dueDate[0] != null) {
                        updatedTask.put("taskDueDate", dueDate[0]);
                    }
                    if (dueTime[0] != null) {
                        Calendar calendarForTime = Calendar.getInstance();
                        calendarForTime.setTime(dueTime[0]);
                        int hours = calendarForTime.get(Calendar.HOUR_OF_DAY);
                        int minutes = calendarForTime.get(Calendar.MINUTE);
                        updatedTask.put("taskDueTime", new Timestamp(hours, minutes));
                    }

                    // Update the task in Firestore
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    String userId = currentUser.getUid();
                    db.collection("users").document(userId).collection("tasks").document(task.taskId)
                            .update(updatedTask)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Tasks.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                                // Optionally fetch and update the UI
                                fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
                                    @Override
                                    public void onSuccess(List<Tasks.Task> tasks) {
                                        updateUI(tasks);
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Toast.makeText(Tasks.this, "Failed to fetch tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(Tasks.this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(Tasks.this, "Error ensuring subject and project: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void findOrCreateSubjectAndProject(String subjectName, String projectName, Date dueDate, Date dueTime,
                                               OnSubjectAndProjectEnsuredListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }
        String userId = currentUser.getUid();

        // Find or create subject
        db.collection("users").document(userId).collection("subjects")
                .whereEqualTo("subjectName", subjectName)
                .get()
                .addOnSuccessListener(subjectQuerySnapshot -> {
                    DocumentReference subjectRef;
                    if (subjectQuerySnapshot.isEmpty()) {
                        // Create new subject with defaults
                        Map<String, Object> subjectData = new HashMap<>();
                        subjectData.put("subjectName", subjectName);
                        subjectData.put("subjectColor", "#b00b00");
                        subjectData.put("subjectSemester", "Fall");
                        subjectData.put("subjectStatus", "Active");

                        subjectRef = db.collection("users").document(userId).collection("subjects").document();
                        subjectRef.set(subjectData)
                                .addOnSuccessListener(aVoid -> {
                                    listener.onReferencesReady(subjectRef, null); // Pass null for projectRef initially
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("CreateTask", "Error creating subject", e);
                                    listener.onFailure("Error creating subject: " + e.getMessage());
                                });
                    } else {
                        subjectRef = subjectQuerySnapshot.getDocuments().get(0).getReference();
                        listener.onReferencesReady(subjectRef, null); // Pass existing subjectRef
                    }

                    // Find or create project
                    db.collection("users").document(userId).collection("projects")
                            .whereEqualTo("projectName", projectName)
                            .get()
                            .addOnSuccessListener(projectQuerySnapshot -> {
                                DocumentReference projectRef;
                                if (projectQuerySnapshot.isEmpty()) {
                                    // Create new project
                                    Map<String, Object> projectData = new HashMap<>();
                                    projectData.put("projectDescription", "None");
                                    projectData.put("projectDueDate", dueDate);
                                    projectData.put("projectDueTime", dueTime);
                                    projectData.put("projectName", projectName);
                                    projectData.put("projectStatus", "Active");
                                    ArrayList<DocumentReference> projectSubjects = new ArrayList<>();
                                    projectSubjects.add(subjectRef);
                                    projectData.put("projectSubjects", projectSubjects);

                                    projectRef = db.collection("users").document(userId).collection("projects").document();
                                    projectRef.set(projectData)
                                            .addOnSuccessListener(aVoid -> {
                                                listener.onReferencesReady(subjectRef, projectRef);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("CreateTask", "Error creating project", e);
                                                listener.onFailure("Error creating project: " + e.getMessage());
                                            });
                                } else {
                                    projectRef = projectQuerySnapshot.getDocuments().get(0).getReference();
                                    listener.onReferencesReady(subjectRef, projectRef);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("CreateTask", "Error finding/creating project", e);
                                listener.onFailure("Error finding/creating project: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateTask", "Error finding/creating subject", e);
                    listener.onFailure("Error finding/creating subject: " + e.getMessage());
                });
    }



    private void fetchTasksForCurrentUser(Firebase.OnTasksFetchedListener listener) {
        firebase.fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
            @Override
            public void onSuccess(List<Tasks.Task> tasks) {
                runOnUiThread(() -> {
                    updateUI(tasks);
                    listener.onSuccess(tasks);
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(Tasks.this, "Failed to fetch tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("FetchTasks", "Error fetching tasks: " + errorMessage);
                    listener.onFailure(errorMessage);
                });
            }
        });
    }




    public static class Task {
        public String taskName; // Task name
        public String taskDescription; // Task description
        public String taskPriority; // Task priority
        public DocumentReference taskProject;
        public DocumentReference taskSubject;
        public String taskProjectString;
        public String taskSubjectString; // Project related to the task
        public String taskStatus; // Task status
        public Date taskDueDate;
        public Date taskDueTime;
        public Date taskStartDate;
        public String taskId;


        // No-argument constructor
        public Task() {}

        public Task(String taskName, String taskDescription, String taskPriority,
                    DocumentReference taskProject, String taskStatus,
                    DocumentReference taskSubject, Date taskDueDate, Date taskStartDate,Date taskDueTime) {
            this.taskName = taskName;
            this.taskDescription = taskDescription;
            this.taskPriority = taskPriority;
            this.taskStatus = taskStatus;
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

        public String getTaskProjectString() {
            return taskProjectString;
        }

        public void setTaskProjectString(String taskProjectString) {
            this.taskProjectString = taskProjectString;
        }

        // Getters and setters for taskProject
        public DocumentReference getTaskProject() {
            return taskProject;
        }

        public void setTaskProject(DocumentReference taskProject) {
            this.taskProject = taskProject;
        }

        // Getters and setters for taskSubjectString
        public String getTaskSubjectString() {
            return taskSubjectString;
        }

        public void setTaskSubjectString(String taskSubjectString) {
            this.taskSubjectString = taskSubjectString;
        }

        // Getters and setters for taskSubject
        public DocumentReference getTaskSubject() {
            return taskSubject;
        }

        public void setTaskSubject(DocumentReference taskSubject) {
            this.taskSubject = taskSubject;
        }

        public void setTaskId(String id) {
        this.taskId = id;
        }
    }

}

