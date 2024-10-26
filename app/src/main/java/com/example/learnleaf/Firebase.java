package com.example.learnleaf;

import android.content.Context;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;



public class Firebase {
    private static final String TAG = "Firebase";
    private static Firebase instance;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final Context context;

    public Firebase(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public interface OnProjectDeletedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    //Used in Login page to check the input email and password, and Sign in
    public void signIn(String email, String password, final AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        callback.onError(Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }

    public static synchronized Firebase getInstance(Context context) {
        if (instance == null) {
            instance = new Firebase(context);
        }
        return instance;
    }

    //Collects user information of currently signed in user from Firestore
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String errorMessage);
    }

    //------------------------------------Projects

    public void createNewProject(String projectName, String subject, String status, OnProjectCreatedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        Projects.Project newProject = new Projects.Project(projectName, status, subject, currentUser.getUid());

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("projects")
                .add(newProject)
                .addOnSuccessListener(documentReference -> {
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure("Error creating project"));
    }

    public interface OnProjectCreatedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public void fetchProjectsForCurrentUser(OnProjectsFetchedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("projects")
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Projects.Project> projects = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Projects.Project project = document.toObject(Projects.Project.class);
                        project.setUserId(document.getId()); // Set the document ID as the project ID
                        projects.add(project);
                    }
                    listener.onSuccess(projects);
                })
                .addOnFailureListener(e -> {
                    listener.onFailure("Failed to fetch projects: " + e.getMessage());
                });
    }

    public interface OnProjectsFetchedListener {
        void onSuccess(List<Projects.Project> projects);
        void onFailure(String errorMessage);
    }


    public void deleteProject(String projectName, String userId, OnProjectDeletedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }



        db.collection("users").document(userId).collection("projects")
                .whereEqualTo("projectName", projectName)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = documentSnapshot.getId();


                        db.collection("users").document(userId).collection("projects").document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onFailure("Error deleting project"));
                    } else {
                        listener.onFailure("Project not found");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure("Error finding project"));
    }

    public void updateProject(String currentProjectName, String newProjectName, String newSubject, String newStatus, OnProjectUpdatedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();


        db.collection("users").document(userId).collection("projects")
                .whereEqualTo("projectName", currentProjectName)
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = documentSnapshot.getId();

                        db.collection("users").document(userId).collection("projects").document(documentId)
                                .update(
                                        "projectName", newProjectName,
                                        "subject", newSubject,
                                        "status", newStatus
                                )
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onFailure("Error updating project"));
                    } else {
                        listener.onFailure("Project not found");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure("Error finding project"));
    }

    public interface OnProjectUpdatedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    //--------------------------------Subjects

    public void updateSubject(Subjects.Subject subject, String newSubjectName, String newSemester, String newColor, String newStatus, OnSubjectUpdatedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("subjects")
                .whereEqualTo("subjectName", subject.getSubjectName())
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = documentSnapshot.getId();

                        db.collection("users").document(userId).collection("subjects").document(documentId)
                                .update(
                                        "subjectName", newSubjectName,
                                        "semester", newSemester,
                                        "subjectColor", newColor,
                                        "status", newStatus
                                )
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onFailure("Error updating subject: " + e.getMessage()));
                    } else {
                        listener.onFailure("Subject not found");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure("Error finding subject: " + e.getMessage()));
    }

    public void createNewSubject(String subjectName, String semester, String color, OnSubjectCreatedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        Subjects.Subject newSubject = new Subjects.Subject(semester, "Active", color, subjectName, currentUser.getUid());

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("subjects")
                .add(newSubject)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure("Error creating subject: " + e.getMessage()));
    }

    public void fetchActiveSubjectsForCurrentUser(OnActiveSubjectsFetchedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("subjects")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Active")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Subjects.Subject> activeSubjects = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Subjects.Subject subject = document.toObject(Subjects.Subject.class);
                        activeSubjects.add(subject);
                    }
                    listener.onSuccess(activeSubjects);
                })
                .addOnFailureListener(e -> listener.onFailure("Failed to fetch subjects: " + e.getMessage()));
    }

    public void deleteSubject(Subjects.Subject subject, OnSubjectDeletedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("subjectss")
                .whereEqualTo("subjectName", subject.getSubjectName())
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = documentSnapshot.getId();

                        db.collection("users").document(userId).collection("subjects").document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onFailure("Error deleting subject: " + e.getMessage()));
                    } else {
                        listener.onFailure("Subject not found");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure("Error finding subject: " + e.getMessage()));
    }

    public interface OnSubjectDeletedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnActiveSubjectsFetchedListener {
        void onSuccess(List<Subjects.Subject> activeSubjects);
        void onFailure(String errorMessage);
    }

    public interface OnSubjectCreatedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnSubjectUpdatedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    //-------------------------------------TASKS

    public void fetchTasksForCurrentUser(OnTasksFetchedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d("TaskFetch", "No current user");
            listener.onSuccess(new ArrayList<>());
            return;
        }

        String userId = currentUser.getUid();
        Log.d("TaskFetch", "Fetching tasks for user ID: " + userId);

        db.collection("users").document(userId).collection("tasks")
                .whereIn("taskStatus", Arrays.asList("Not Started", "Active", "In Progress"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Tasks.Task> tasks = new ArrayList<>();
                    Log.d("TaskFetch", "Query returned " + queryDocumentSnapshots.size() + " documents");
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d("TaskFetch", "Processing document: " + document.getId());
                        try {
                            Tasks.Task task = new Tasks.Task();
                            task.setId(document.getId());
                            task.setAssignment(document.getString("taskName"));
                            task.setStatus(document.getString("taskStatus"));

                            // Handle subject reference
                            DocumentReference subjectRef = document.getDocumentReference("taskSubject");
                            if (subjectRef != null) {
                                task.setSubject(subjectRef.getId());
                            }

                            // Set userId (if needed)
                            task.setUserId(userId);

                            // Handle dueDate if it exists in your Firebase document
                            Timestamp dueDate = document.getTimestamp("dueDate");
                            if (dueDate != null) {
                                task.setDueDate(dueDate);
                            }

                            Log.d("TaskFetch", "Fetched task - Assignment: " + task.getAssignment() +
                                    ", Status: " + task.getStatus() +
                                    ", Subject: " + task.getSubject() +
                                    ", DueDate: " + (task.getDueDate() != null ? task.getDueDateAsDate() : "null"));
                            tasks.add(task);
                        } catch (Exception e) {
                            Log.e("TaskFetch", "Error processing document " + document.getId(), e);
                        }
                    }
                    Log.d("TaskFetch", "Total tasks fetched: " + tasks.size());
                    listener.onSuccess(tasks);
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskFetch", "Error fetching tasks", e);
                    listener.onFailure("Failed to fetch tasks: " + e.getMessage());
                });
    }

    public void deleteTask(String taskId, OnTaskDeletedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onFailure("Error: Task ID is null or empty");
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure("Error deleting task: " + e.getMessage()));
    }

    public interface OnTaskDeletedListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }
    public interface OnTasksFetchedListener {
        void onSuccess(List<Tasks.Task> tasks);
        void onFailure(String errorMessage);
    }


}