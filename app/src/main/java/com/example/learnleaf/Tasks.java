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
import androidx.cardview.widget.CardView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import com.google.firebase.firestore.Query;

public class Tasks extends AppCompatActivity {
    private LinearLayout tasksContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasks);

        tasksContainer = findViewById(R.id.tasksContainer);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fetchTasksForCurrentUser();
    }


    public void onAddNewTaskClick(View view) {
        showCreateTaskDialog();
    }

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
                    dateView.setTag(selectedDate); // Store the Date object as a tag
                }, year, month, day);
        datePickerDialog.show();
    }


    private void showCreateTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Task");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_task, null);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);
        final EditText projectInput = viewInflated.findViewById(R.id.projectInput);
        final EditText assignmentInput = viewInflated.findViewById(R.id.assignmentInput);
        final EditText descriptionInput = viewInflated.findViewById(R.id.descriptionInput);
        final TextView startDateInput = viewInflated.findViewById(R.id.startDateInput);
        final TextView dueDateInput = viewInflated.findViewById(R.id.dueDateInput);
        final Spinner prioritySpinner = viewInflated.findViewById(R.id.prioritySpinner);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        startDateInput.setOnClickListener(v -> showDatePickerDialog(startDateInput));
        dueDateInput.setOnClickListener(v -> showDatePickerDialog(dueDateInput));

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
            String subject = subjectInput.getText().toString();
            String project = projectInput.getText().toString();
            String assignment = assignmentInput.getText().toString();
            String description = descriptionInput.getText().toString();
            String priority = prioritySpinner.getSelectedItem().toString();
            String status = statusSpinner.getSelectedItem().toString();

            Date startDate = parseDateString(startDateInput.getText().toString());
            Date dueDate = parseDateString(dueDateInput.getText().toString());

            if (startDate == null || dueDate == null) {
                Toast.makeText(Tasks.this, "Invalid date format. Please use yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                return;
            }

            createNewTask(subject, project, assignment, description, startDate, dueDate, priority, status);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createNewTask(String subject, String project, String assignment, String description,
                               Date startDate, Date dueDate, String priority, String status) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task();
        newTask.subject = subject;
        newTask.project = project;
        newTask.assignment = assignment;
        newTask.description = description;
        newTask.startDate = new Timestamp(startDate);
        newTask.dueDate = new Timestamp(dueDate);
        newTask.priority = priority;
        newTask.status = status;
        newTask.userId = currentUser.getUid();

        db.collection("tasks")
                .add(newTask)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(Tasks.this, "Task created successfully", Toast.LENGTH_SHORT).show();
                    fetchTasksForCurrentUser(); // Refresh the task list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Tasks.this, "Error creating task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void fetchTasksForCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d("TaskFetch", "No current user");
            updateUI(new ArrayList<>()); // Update UI with empty list
            return;
        }

        String userId = currentUser.getUid();
        Log.d("TaskFetch", "Fetching tasks for user ID: " + userId);

        // Show loading indicator
        //showLoadingIndicator();

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereIn("status", Arrays.asList("Not Started", "Active", "In Progress"))
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    Log.d("TaskFetch", "Query returned " + queryDocumentSnapshots.size() + " documents");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d("TaskFetch", "Processing document: " + document.getId());
                        try {
                            Task task = new Task();
                            task.id = document.getId();
                            task.assignment = document.getString("assignment");
                            task.description = document.getString("description");
                            task.dueDate = document.getTimestamp("dueDate");
                            task.priority = document.getString("priority");
                            task.project = document.getString("project");
                            task.startDate = document.getTimestamp("startDate");
                            task.status = document.getString("status");
                            task.subject = document.getString("subject");
                            task.userId = document.getString("userId");

                            Log.d("TaskFetch", "Fetched task - Assignment: " + task.assignment +
                                    ", Status: " + task.status +
                                    ", UserId: " + task.userId +
                                    ", DueDate: " + (task.dueDate != null ? task.dueDate.toDate() : "null"));
                            tasks.add(task);
                        } catch (Exception e) {
                            Log.e("TaskFetch", "Error processing document " + document.getId(), e);
                        }
                    }
                    Log.d("TaskFetch", "Total tasks fetched: " + tasks.size());
                    updateUI(tasks);
                    //hideLoadingIndicator();
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskFetch", "Error fetching tasks", e);
                    Toast.makeText(Tasks.this, "Failed to fetch tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    updateUI(new ArrayList<>()); // Update UI with empty list
                    //hideLoadingIndicator();
                });
    }

    private void updateUI(List<Task> tasks) {
        tasksContainer.removeAllViews(); // Clear existing views

        for (Task task : tasks) {
            View taskView = getLayoutInflater().inflate(R.layout.task_item_block, null);

            // Populate the view with task data
            TextView assignmentTextView = taskView.findViewById(R.id.assignmentTextView);
            assignmentTextView.setText(task.assignment);

            TextView subjectTextView = taskView.findViewById(R.id.subjectTextView);
            subjectTextView.setText(task.subject);

            TextView projectTextView = taskView.findViewById(R.id.projectTextView);
            projectTextView.setText(task.project);

            TextView descriptionTextView = taskView.findViewById(R.id.descriptionTextView);
            descriptionTextView.setText(task.description);

            TextView priorityTextView = taskView.findViewById(R.id.priorityTextView);
            priorityTextView.setText(task.priority);

            TextView statusTextView = taskView.findViewById(R.id.statusTextView);
            statusTextView.setText(task.status);

            TextView startDateTextView = taskView.findViewById(R.id.startDateTextView);
            startDateTextView.setText(task.startDate != null ? task.startDate.toDate().toString() : "Not set");

            TextView dueDateTextView = taskView.findViewById(R.id.dueDateTextView);
            dueDateTextView.setText(task.dueDate != null ? task.dueDate.toDate().toString() : "Not set");

            ImageButton editButton = taskView.findViewById(R.id.editButton);
            editButton.setOnClickListener(v -> showEditTaskDialog(task));

            ImageButton deleteButton = taskView.findViewById(R.id.deleteButton);
            deleteButton.setOnClickListener(v -> deleteTask(task, taskView));

            tasksContainer.addView(taskView);
        }
    }

    private void deleteTask(Task task, View taskView) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (task.id == null) {
                        Toast.makeText(this, "Error: Task ID is null", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("tasks").document(task.id)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(Tasks.this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
                                // Remove the task view from the container
                                tasksContainer.removeView(taskView);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(Tasks.this, "Error deleting task: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Task");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null);
        final EditText assignmentInput = viewInflated.findViewById(R.id.assignmentInput);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);
        final EditText projectInput = viewInflated.findViewById(R.id.projectInput);
        final EditText descriptionInput = viewInflated.findViewById(R.id.descriptionInput);
        final EditText startDateInput = viewInflated.findViewById(R.id.startDateInput);
        final EditText dueDateInput = viewInflated.findViewById(R.id.dueDateInput);
        final Spinner prioritySpinner = viewInflated.findViewById(R.id.prioritySpinner);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // Pre-fill the fields with current task data
        assignmentInput.setText(task.assignment);
        subjectInput.setText(task.subject);
        projectInput.setText(task.project);
        descriptionInput.setText(task.description);
        startDateInput.setText(task.startDate != null ? task.startDate.toDate().toString() : "");
        dueDateInput.setText(task.dueDate != null ? task.dueDate.toDate().toString() : "");

        // Set up spinners
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(priorityAdapter.getPosition(task.priority));

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setSelection(statusAdapter.getPosition(task.status));

        builder.setView(viewInflated);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newAssignment = assignmentInput.getText().toString();
            String newSubject = subjectInput.getText().toString();
            String newProject = projectInput.getText().toString();
            String newDescription = descriptionInput.getText().toString();
            String newPriority = prioritySpinner.getSelectedItem().toString();
            String newStatus = statusSpinner.getSelectedItem().toString();

            // Parse dates (you might want to use a DatePicker instead)
            Date newStartDate = parseDate(startDateInput.getText().toString());
            Date newDueDate = parseDate(dueDateInput.getText().toString());

            updateTask(task, newAssignment, newSubject, newProject, newDescription,
                    newStartDate, newDueDate, newPriority, newStatus);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }



    private void updateTask(Task task, String newAssignment, String newSubject, String newProject,
                            String newDescription, Date newStartDate, Date newDueDate,
                            String newPriority, String newStatus) {
        // Update the task object
        task.assignment = newAssignment;
        task.subject = newSubject;
        task.project = newProject;
        task.description = newDescription;
        task.startDate = newStartDate != null ? new Timestamp(newStartDate) : null;
        task.dueDate = newDueDate != null ? new Timestamp(newDueDate) : null;
        task.priority = newPriority;
        task.status = newStatus;

        // Update the task in Firestore
        db.collection("tasks").document(task.id)
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Tasks.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                    fetchTasksForCurrentUser(); // Refresh the task list
                })
                .addOnFailureListener(e -> Toast.makeText(Tasks.this, "Error updating task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }



    private Date parseDate(String dateString) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
            return format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }





        // Constructor with all fields in the specified order
        public class Task {
            public String id;
            public String userId;
            public String assignment;
            public String description;
            public Timestamp startDate;
            public Timestamp dueDate;
            public String priority;
            public String status;
            public String subject;
            public String project;

            // No-argument constructor
            public Task() {}


            public Task(String subject, String project, String assignment, String description,
                        Timestamp startDate, Timestamp dueDate,
                        String priority, String status, String userId) {
                this.assignment = assignment;
                this.description = description;
                this.dueDate = dueDate;
                this.priority = priority;
                this.project = project;
                this.startDate = startDate;
                this.status = status;
                this.subject = subject;
                this.userId = userId;
            }

            // Getters and setters
            public String getAssignment() {
                return assignment;
            }

            public void setAssignment(String assignment) {
                this.assignment = assignment;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getPriority() {
                return priority;
            }

            public void setPriority(String priority) {
                this.priority = priority;
            }

            public String getProject() {
                return project;
            }

            public void setProject(String project) {
                this.project = project;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public String getSubject() {
                return subject;
            }

            public void setSubject(String subject) {
                this.subject = subject;
            }

            public String getUserId() {
                return userId;
            }

            public void setUserId(String userId) {
                this.userId = userId;
            }

            public Timestamp getStartDate() {
                return startDate;
            }

            public void setStartDate(Timestamp startDate) {
                this.startDate = startDate;
            }

            public Timestamp getDueDate() {
                return dueDate;
            }

            public void setDueDate(Timestamp dueDate) {
                this.dueDate = dueDate;
            }

            @Exclude
            public Date getStartDateAsDate() {
                return startDate != null ? startDate.toDate() : null;
            }

            @Exclude
            public void setStartDateFromDate(Date date) {
                this.startDate = date != null ? new Timestamp(date) : null;
            }

            @Exclude
            public Date getDueDateAsDate() {
                return dueDate != null ? dueDate.toDate() : null;
            }

            @Exclude
            public void setDueDateFromDate(Date date) {
                this.dueDate = date != null ? new Timestamp(date) : null;
            }

            @Exclude
            public String getFormattedStartDate() {
                if (startDate == null) return null;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return sdf.format(startDate.toDate());
            }

            @Exclude
            public String getFormattedDueDate() {
                if (dueDate == null) return null;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return sdf.format(dueDate.toDate());
            }

            @Exclude
            public void setStartDateFromString(String dateString) throws ParseException {
                if (dateString == null) {
                    this.startDate = null;
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date date = sdf.parse(dateString);
                    this.startDate = new Timestamp(date);
                }
            }

            @Exclude
            public void setDueDateFromString(String dateString) throws ParseException {
                if (dateString == null) {
                    this.dueDate = null;
                } else {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date date = sdf.parse(dateString);
                    this.dueDate = new Timestamp(date);
                }
            }
        }

    }

