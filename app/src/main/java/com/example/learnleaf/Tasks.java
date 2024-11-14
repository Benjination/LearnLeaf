package com.example.learnleaf;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Build;
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
import com.google.firebase.firestore.DocumentReference;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.material.icons.sharp.EditCalendarKt;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Tasks extends AppCompatActivity {
    private LinearLayout tasksContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Firebase firebase;
    private List<Task> filteredTasks;
    private List<Task> allTasks;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasks);

        tasksContainer = findViewById(R.id.tasksContainer);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebase = new Firebase(this);
        tasksContainer = findViewById(R.id.tasksContainer);
        Button filter = findViewById(R.id.filter);

        filter.setOnClickListener(v -> {
            showSearchByDialog();
        });

        ImageView addNew = findViewById(R.id.addNewTask);
        allTasks = new ArrayList<>();
        filteredTasks = new ArrayList<>();

//        //Calls method in firebase to update local database
//        fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
//            @Override
//            public void onSuccess(List<Tasks.Task> tasks) {
//                updateUI(tasks);
//            }
//
//            @Override
//            public void onFailure(String errorMessage) {
//                Toast.makeText(Tasks.this, "Failed to fetch tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
//                Log.e("FetchTasks", "Error fetching tasks: " + errorMessage);
//            }
//        });

        addNew.setOnClickListener(v -> showCreateTaskDialog());
        loadTasks();
    }

    private void loadTasks() {
        firebase.fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
            @Override
            public void onSuccess(List<Task> tasks) {
                allTasks.clear();
                allTasks.addAll(tasks);
                filteredTasks.clear();
                filteredTasks.addAll(tasks);
                runOnUiThread(() -> updateUI(filteredTasks));
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(Tasks.this, "Failed to fetch tasks: " + errorMessage, Toast.LENGTH_SHORT).show();
                    Log.e("FetchTasks", "Error fetching tasks: " + errorMessage);
                });
            }
        });
    }

    private void showSearchByDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Tasks");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_filter_task, null);
        Spinner searchSpinner = viewInflated.findViewById(R.id.searchSpinner);
        EditText searchInput = viewInflated.findViewById(R.id.search);
        Button searchButton = viewInflated.findViewById(R.id.submit);
        Button clearFilters = viewInflated.findViewById(R.id.clear);

        ArrayAdapter<CharSequence> searchAdapter = ArrayAdapter.createFromResource(this,
                R.array.search_array, android.R.layout.simple_spinner_item);
        searchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchSpinner.setAdapter(searchAdapter);

        builder.setView(viewInflated);

        AlertDialog dialog = builder.create();

        searchButton.setOnClickListener(v -> {
            String selectedOption = searchSpinner.getSelectedItem().toString();
            String searchText = searchInput.getText().toString().trim().toLowerCase();

            if (selectedOption.equals("Not Selected")) {
                Toast.makeText(Tasks.this, "You must first select a search option", Toast.LENGTH_SHORT).show();
            } else if (searchText.isEmpty()) {
                Toast.makeText(Tasks.this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            } else {
                switch (selectedOption) {
                    case "Subject":
                        searchTaskSubjects(searchText);
                        break;
                    case "Project":
                        searchTaskProjects(searchText);
                        break;
                    case "Priority":
                        searchPriority(searchText);
                        break;
                    case "Status":
                        searchStatus(searchText);
                        break;
                    default:
                        Toast.makeText(Tasks.this, "Invalid search option", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        });

        clearFilters.setOnClickListener(v -> {
            filteredTasks.clear();
            filteredTasks.addAll(allTasks);
            updateUI(filteredTasks);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void searchTaskSubjects(String searchText) {
        filteredTasks.clear();
        for (Task task : allTasks) {
            String subjectString = task.getTaskSubjectString();
            if (subjectString != null && subjectString.toLowerCase().contains(searchText.toLowerCase())) {
                filteredTasks.add(task);
            }
        }
        updateUI(filteredTasks);
    }

    private void searchTaskProjects(String searchText) {
        filteredTasks.clear();
        for (Task task : allTasks) {
            String projectString = task.getTaskProjectString();
            if (projectString != null && projectString.toLowerCase().contains(searchText.toLowerCase())) {
                filteredTasks.add(task);
            }
        }
        updateUI(filteredTasks);
    }

    private void searchPriority(String searchText) {
        filteredTasks.clear();
        for (Task task : allTasks) {
            if (task.getTaskPriority().toLowerCase().contains(searchText)) {
                filteredTasks.add(task);
            }
        }
        updateUI(filteredTasks);
    }

    private void searchStatus(String searchText) {
        filteredTasks.clear();
        for (Task task : allTasks) {
            if (task.getTaskStatus().toLowerCase().contains(searchText)) {
                filteredTasks.add(task);
            }
        }
        updateUI(filteredTasks);
    }



    /////////////////////////////////////////////////////////////////////////////////////////////////

    //Submenu that allows users to input all data for a new task
    private void showCreateTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Task");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_task, null);

        EditText subjectInput = viewInflated.findViewById(R.id.taskSubjectTextView);
        EditText projectInput = viewInflated.findViewById(R.id.taskProjectTextView);
        EditText assignmentInput = viewInflated.findViewById(R.id.taskNameTextView);
        EditText descriptionInput = viewInflated.findViewById(R.id.taskDescriptionInput);
        Spinner prioritySpinner = viewInflated.findViewById(R.id.prioritySpinner);
        Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);
        Button startDateButton = viewInflated.findViewById(R.id.startDateButton);
        Button dueDateButton = viewInflated.findViewById(R.id.dueDateButton);
        Button dueTimeButton = viewInflated.findViewById(R.id.dueTimeButton);

        //Set up priority and status spinners
        setupSpinners(prioritySpinner, statusSpinner);

        //Variables to store date and time
        final Calendar calendar = Calendar.getInstance();
        final Date[] startDate = {null};
        final Date[] dueDate = {null};
        final Date[] dueTime = {null};

        //Allows user to use calendars and clocks to input timestamps
        setupDateTimePickers(startDateButton, dueDateButton, dueTimeButton, calendar, startDate, dueDate, dueTime);

        builder.setView(viewInflated);

        builder.setPositiveButton("Create", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);

            //User will not be able to add new task if assignment is blank
            assignmentInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    positiveButton.setEnabled(s.toString().trim().length() > 0);
                }
            });

            positiveButton.setOnClickListener(v -> {
                String taskName = assignmentInput.getText().toString().trim();
                String taskDescription = descriptionInput.getText().toString().trim();
                String taskPriority = prioritySpinner.getSelectedItem().toString();
                String taskProject = projectInput.getText().toString().trim();
                String taskSubject = subjectInput.getText().toString().trim();
                String taskStatus = statusSpinner.getSelectedItem().toString();

                List<Projects.Project> localProjects = getLocalProjects();
                List<Subjects.Subject> activeSubjects = getActiveSubjects();

                firebase.createNewTask(taskName, taskDescription, taskProject, taskSubject, taskPriority, taskStatus,
                        startDate[0], dueDate[0], dueTime[0], localProjects, activeSubjects,
                        new Firebase.OnTaskCreatedListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(Tasks.this, "Task created successfully", Toast.LENGTH_SHORT).show();
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
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(Tasks.this, "Failed to create task: " + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        });

        dialog.show();
    }

    //arrays for spinners in res>values>arrays
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

    //Adds calendar selection for user and ensures correct data format is submitted
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

        //Adds clock feature for user and ensures correct data format is submitted
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

    //fetches tasks from local database
    private List<Projects.Project> getLocalProjects() {
        if (Firebase.localProjects == null || Firebase.localProjects.isEmpty()) {
            fetchProjects();
        }
        return Firebase.localProjects;
    }

    //collects subjects from local database
    private List<Subjects.Subject> getActiveSubjects() {
        if (Firebase.localSubjects == null || Firebase.localSubjects.isEmpty()) {
            fetchActiveSubjects();
        }
        return Firebase.localSubjects;
    }

    //fetches all projects from the Firebase and adds to local database
    private void fetchProjects() {
        firebase.fetchProjectsForCurrentUser(new Firebase.OnProjectsFetchedListener() {
            @Override
            public void onSuccess(List<Projects.Project> projects) {}
            @Override
            public void onFailure(String errorMessage) {
                Log.e("ProjectFetch", "Failed to fetch projects: " + errorMessage);
            }
        });
    }

    //Gathers subjects from Firebase and adds them to local database
    private void fetchActiveSubjects() {
        firebase.fetchAllSubjectsForCurrentUser(new Firebase.OnAllSubjectsFetchedListener() {
            @Override
            public void onSuccess(List<Subjects.Subject> subjects) {
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("SubjectFetch", "Failed to fetch subjects: " + errorMessage);
                runOnUiThread(() -> Toast.makeText(Tasks.this, "Failed to fetch subjects: " + errorMessage, Toast.LENGTH_SHORT).show());
            }
        });
    }


    //data checks
    public interface OnSubjectAndProjectEnsuredListener {
        void onReferencesReady(DocumentReference subjectRef, DocumentReference projectRef);
        void onFailure(String errorMessage);}



    //This method is used to update the Tasks Scrollview on Display
    private void updateUI(List<Task> tasks) {
        runOnUiThread(() -> {
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

            for (Task task : tasks) {
                View taskView = getLayoutInflater().inflate(R.layout.task_item_block, null);

                TextView assignmentTextView = taskView.findViewById(R.id.taskNameTextView);
                assignmentTextView.setText(task.taskName);

                TextView descriptionTextView = taskView.findViewById(R.id.taskDescriptionTextView);
                descriptionTextView.setText(task.taskDescription);

                TextView priorityTextView = taskView.findViewById(R.id.taskPriorityTextView);
                priorityTextView.setText(task.taskPriority);

                TextView projectTextView = taskView.findViewById(R.id.taskProjectTextView);
                TextView subjectTextView = taskView.findViewById(R.id.taskSubjectTextView);

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

                TextView startDateTextView = taskView.findViewById(R.id.startDateTextView);
                if (task.taskStartDate != null) {
                    startDateTextView.setText("Start: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.taskStartDate));
                } else {
                    startDateTextView.setText("Start: Not set");
                }

                TextView dueDateTextView = taskView.findViewById(R.id.dueDateTextView);
                if (task.taskDueDate != null) {
                    dueDateTextView.setText("Due: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.taskDueDate));
                } else {
                    dueDateTextView.setText("Due: Not set");
                }

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
        });
    }

    //Allows user to delete existing task and confirms with user before permanantly deleting
    private void deleteTask(Tasks.Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            if (task.taskId == null) {
                Toast.makeText(this, "Error: Task ID is null", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).collection("tasks").document(task.taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Tasks.this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                        fetchTasksForCurrentUser(new Firebase.OnTasksFetchedListener() {
                            @Override
                            public void onSuccess(List<Tasks.Task> tasks) {
                                updateUI(tasks); //updates task display after deletion
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

    //Submenu dialog_edit_task allows user to edit an existing task
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

        Button startDateButton = viewInflated.findViewById(R.id.startDateButton);
        Button dueDateButton = viewInflated.findViewById(R.id.dueDateButton);
        Button dueTimeButton = viewInflated.findViewById(R.id.dueTimeButton);

        //fills editViews with existing data before edit
        assignmentInput.setText(task.taskName);
        descriptionInput.setText(task.taskDescription);

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

        // Set up priority and status spinners both in res>values>arrays
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

        final Calendar calendar = Calendar.getInstance();
        final Date[] startDate = {task.taskStartDate};
        final Date[] dueDate = {task.taskDueDate};
        final Date[] dueTime = {task.taskDueTime};

        if (startDate[0] != null) {
            startDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate[0]));
        }
        if (dueDate[0] != null) {
            dueDateButton.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dueDate[0]));
        }
        if (dueTime[0] != null) {
            dueTimeButton.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueTime[0]));
        }

        //listeners for date and time features
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

            firebase.findOrCreateSubjectAndProject(newSubjectName, newProjectName, dueDate[0], dueTime[0], new OnSubjectAndProjectEnsuredListener() {
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



                    String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                    db.collection("users").document(userId).collection("tasks").document(task.taskId)
                            .update(updatedTask)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Tasks.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
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







    //call to method in Firebase.java
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


    //Task object definition
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


        //Empty constructor required for Firebase
        //Do not delete
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

        // Setters
        public void setTaskProjectString(String taskProjectString) {this.taskProjectString = taskProjectString;}
        public void setTaskSubjectString(String taskSubjectString) {this.taskSubjectString = taskSubjectString;}
        public void setTaskId(String id) {
            this.taskId = id;
        }

        //Getters
        public DocumentReference getTaskProject() {
            return taskProject;
        }
        public String getTaskProjectString(){return taskProjectString;}
        public DocumentReference getTaskSubject() {
            return taskSubject;
        }
        public String getTaskSubjectString(){return taskSubjectString;}
        public String getTaskName(){return taskName;}
        public String getTaskDescription(){return taskDescription;}
        public String getTaskPriority() {return taskPriority;}
        public String getTaskStatus() {return taskStatus;}
    }

}

