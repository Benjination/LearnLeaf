package com.example.learnleaf;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class Tasks extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout tasksContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasks);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        tasksContainer = findViewById(R.id.tasksContainer);
        ImageView addNewTask = findViewById(R.id.addNewTask);

        addNewTask.setOnClickListener(v -> showCreateTaskDialog());

        fetchTasksForCurrentUser();
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

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String assignment = assignmentInput.getText().toString();
            String description = descriptionInput.getText().toString();
            //String dueDateString = dueDateInput.getText().toString();
            String priority = prioritySpinner.getSelectedItem().toString();
            String project = projectInput.getText().toString();
            //String startDateString = startDateInput.getText().toString();
            String status = statusSpinner.getSelectedItem().toString();
            String subject = subjectInput.getText().toString();


            Date startDate = parseDateString(startDateInput.getText().toString());
            Date dueDate = parseDateString(dueDateInput.getText().toString());
            createNewTask(subject, project, assignment, description,
                    startDate, dueDate, priority, status);

            if (startDate == null || dueDate == null) {
                Toast.makeText(Tasks.this, "Invalid date format. Please use yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                return;
            }

            createNewTask(subject, project, assignment, description, startDate, dueDate, priority, status);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private Date parseDateString(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormat.setLenient(false); // This will make the parser strict

        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            Log.e("Tasks", "Error parsing date: " + dateString, e);
            // You can choose to return null or throw an exception
            // return null;
            throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd.");
        }
    }




    private void createNewTask(String subject, String project, String assignment, String description,
                               Date startDate, Date dueDate, String priority, String status) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(assignment, description, dueDate, priority, project, startDate,  status, subject, currentUser.getUid());

        db.collection("tasks")
                .add(newTask)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(Tasks.this, "Task created successfully", Toast.LENGTH_SHORT).show();
                    fetchTasksForCurrentUser(); // Refresh the task list
                })
                .addOnFailureListener(e -> Toast.makeText(Tasks.this, "Error creating task", Toast.LENGTH_SHORT).show());
    }

    private void fetchTasksForCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();
        Log.d("TaskFetch", "Current user ID: " + userId);

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereIn("status", Arrays.asList("Not Started", "Active", "In Progress"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> tasks = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Task task = document.toObject(Task.class);
                        Log.d("TaskFetch", "Fetched task - UserId: " + task.getUserId() +
                                ", Assignment: " + task.getAssignment() +
                                ", Status: " + task.getStatus());
                        tasks.add(task);
                    }
                    Log.d("TaskFetch", "Total tasks fetched: " + tasks.size());
                    updateUI(tasks);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Tasks.this, "Failed to fetch tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TaskFetch", "Error fetching tasks", e);
                });
    }

    private void updateUI(List<Task> tasks) {
        tasksContainer.removeAllViews();

        if (tasks.isEmpty()) {
            TextView noTasksText = new TextView(this);
            noTasksText.setText(R.string.no_tasks_found);
            tasksContainer.addView(noTasksText);
            return;
        }

        for (Task task : tasks) {
            View taskView = getLayoutInflater().inflate(R.layout.task_item_block, tasksContainer, false);

            CardView cardView = taskView.findViewById(R.id.taskCardView);
            TextView assignmentTextView = taskView.findViewById(R.id.assignmentTextView);
            TextView projectTextView = taskView.findViewById(R.id.projectTextView);
            TextView subjectTextView = taskView.findViewById(R.id.subjectTextView);
            TextView descriptionTextView = taskView.findViewById(R.id.descriptionTextView);
            TextView priorityTextView = taskView.findViewById(R.id.priorityTextView);
            TextView statusTextView = taskView.findViewById(R.id.statusTextView);

            assignmentTextView.setText(task.getAssignment());
            projectTextView.setText(String.format("%s%s", getString(R.string.project), task.getProject()));
            subjectTextView.setText(String.format("%s%s", getString(R.string.subject), task.getSubject()));
            descriptionTextView.setText(task.getDescription());
            priorityTextView.setText(String.format("%s%s", getString(R.string.priority), task.getPriority()));
            statusTextView.setText(String.format("%s%s", getString(R.string.status), task.getStatus()));


            String contentDescription = String.format("Task: %s, Project: %s, Subject: %s, Priority: %s, Status: %s",
                    task.getAssignment(), task.getProject(), task.getSubject(), task.getPriority(), task.getStatus());
            cardView.setContentDescription(contentDescription);

            taskView.setOnClickListener(v -> showEditTaskDialog(task));

            tasksContainer.addView(taskView);
        }
    }

    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Task");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_task, null);
        final EditText subjectInput = viewInflated.findViewById(R.id.subjectInput);
        final EditText projectInput = viewInflated.findViewById(R.id.projectInput);
        final EditText assignmentInput = viewInflated.findViewById(R.id.assignmentInput);
        final EditText descriptionInput = viewInflated.findViewById(R.id.descriptionInput);
        final EditText startDateInput = viewInflated.findViewById(R.id.startDateInput);
        final EditText dueDateInput = viewInflated.findViewById(R.id.dueDateInput);
        final Spinner prioritySpinner = viewInflated.findViewById(R.id.prioritySpinner);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);

        // Pre-fill the fields with current task data
        subjectInput.setText(task.getSubject());
        projectInput.setText(task.getProject());
        assignmentInput.setText(task.getAssignment());
        descriptionInput.setText(task.getDescription());
        startDateInput.setText(task.getStartDate());
        dueDateInput.setText(task.getDueDate());

        // Set up spinners
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_array, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
        prioritySpinner.setSelection(priorityAdapter.getPosition(task.getPriority()));

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        statusSpinner.setSelection(statusAdapter.getPosition(task.getStatus()));

        builder.setView(viewInflated);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String subject = subjectInput.getText().toString();
            String project = projectInput.getText().toString();
            String assignment = assignmentInput.getText().toString();
            String description = descriptionInput.getText().toString();
            String priority = prioritySpinner.getSelectedItem().toString();
            String status = statusSpinner.getSelectedItem().toString();
            Date startDate = parseDateString(startDateInput.getText().toString());
            Date dueDate = parseDateString(dueDateInput.getText().toString());

            updateTask(task, assignment, description, dueDate, priority,
                    project, startDate, status, subject);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateTask(Task task, String assignment, String description, Date dueDate, String priority,
                            String project, Date startDate, String status, String subject) {

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Update the task object
        task.setSubject(subject);
        task.setProject(project);
        task.setAssignment(assignment);
        task.setDescription(description);
        task.setStartDate(startDate);
        task.setDueDate(dueDate);
        task.setPriority(priority);
        task.setStatus(status);

        // Update the task in Firestore
        db.collection("tasks").document(currentUser.getUid())
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Tasks.this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                    fetchTasksForCurrentUser(); // Refresh the task list
                })
                .addOnFailureListener(e -> Toast.makeText(Tasks.this, "Error updating task", Toast.LENGTH_SHORT).show());
    }

    // Task class to represent the data model
    public static class Task {
        private String assignment;
        private String description;
        private Date dueDate;
        private String priority;
        private String project;
        private Date startDate;
        private String status;
        private String subject;
        private String userId;

        /** @noinspection unused*/ // Default constructor (required for Firestore)
        public Task() {}

        // Constructor with all fields in the specified order
        public Task(String assignment, String description, Date dueDate, String priority,
                    String project, Date startDate, String status, String subject, String userId) {
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

        // Getters and setters (in the same order)
        public String getAssignment() { return assignment; }
        public void setAssignment(String assignment) { this.assignment = assignment; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }


        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getProject() { return project; }
        public void setProject(String project) { this.project = project; }


        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getUserId() { return userId; }

        public String getStartDate() { return startDate.toString(); }
        public void setStartDate(Date startDate) { this.startDate = startDate; }

        public String getDueDate() { return dueDate.toString(); }
        public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    }
}