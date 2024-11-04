package com.example.learnleaf;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;

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
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


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

    public static ArrayList<Subjects.Subject> localSubjects = new ArrayList<>();
    public static ArrayList<Projects.Project> localProjects = new ArrayList<>();

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
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


    public interface AuthCallback {
        void onSuccess(FirebaseUser user);

        void onError(String errorMessage);
    }

    //------------------------------------Projects

    public void createNewProject(String projectName, String projectDescription, String projectStatus,
                                 List<DocumentReference> subjectIds, Date projectDueDate,
                                 Date projectDueTime, OnProjectCreatedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        // Create a Map to hold all project attributes
        Map<String, Object> newProject = new HashMap<>();
        newProject.put("projectName", projectName);
        newProject.put("projectDescription", projectDescription);
        newProject.put("projectStatus", projectStatus);
        newProject.put("projectSubjects", subjectIds);
        newProject.put("projectDueDate", projectDueDate != null ? new Timestamp(projectDueDate) : null);
        newProject.put("projectDueTime", projectDueTime != null ? new Timestamp(projectDueTime) : null);

        // Add the new project to Firestore
        db.collection("users").document(userId).collection("projects")
                .add(newProject)
                .addOnSuccessListener(documentReference -> {
                    // Add the project ID to the map after successful creation
                    newProject.put("projectId", documentReference.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> listener.onFailure("Error creating project: " + e.getMessage()));
    }


    public void fetchProjectsForCurrentUser(OnProjectsFetchedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        db.collection("users").document(currentUser.getUid()).collection("projects")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    localProjects.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Projects.Project project = document.toObject(Projects.Project.class);
                        if (project != null) {
                            project.setProjectId(document.getId()); // Correctly using the setter method
                            localProjects.add(project);
                        }
                    }
                    listener.onSuccess(localProjects);
                })
                .addOnFailureListener(e -> {
                    listener.onFailure("Failed to fetch projects: " + e.getMessage());
                });
    }


    public void deleteProject(String projectName, OnProjectDeletedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        // Query to find the project by name and user ID
        db.collection("users").document(userId).collection("projects")
                .whereEqualTo("projectName", projectName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                        String documentId = documentSnapshot.getId(); // Get document ID

                        // Delete the project document
                        db.collection("users").document(userId).collection("projects").document(documentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onFailure("Error deleting project: " + e.getMessage()));
                    } else {
                        listener.onFailure("Project not found");
                    }
                })
                .addOnFailureListener(e -> listener.onFailure("Error finding project: " + e.getMessage()));
    }


    public interface OnProjectCreatedListener {
        void onSuccess();

        void onFailure(String errorMessage);
    }

    public interface OnProjectDeletedListener {
        void onSuccess();

        void onFailure(String errorMessage);
    }


    public interface OnProjectsFetchedListener {
        void onSuccess(List<Projects.Project> projects);

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
                        String documentId = documentSnapshot.getId(); // Get the document ID

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

        // Create a Map instead of using a Subject object
        Map<String, Object> newSubject = new HashMap<>();
        newSubject.put("subjectSemester", semester);
        newSubject.put("subjectStatus", "Active");
        newSubject.put("subjectColor", color);
        newSubject.put("subjectName", subjectName);

        db.collection("users").document(currentUser.getUid()).collection("subjects")
                .add(newSubject)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onFailure("Error creating subject: " + e.getMessage()));
    }


    public void fetchAllSubjectsForCurrentUser(OnAllSubjectsFetchedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        db.collection("users").document(currentUser.getUid()).collection("subjects")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Subjects.Subject> allSubjects = new ArrayList<>();
                    // Clear the existing local storage array before adding new subjects
                    localSubjects.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Subjects.Subject subject = document.toObject(Subjects.Subject.class);
                        if (subject != null) {
                            subject.subjectId = document.getId(); // Ensure the ID is set
                            allSubjects.add(subject);
                            // Add the subject to the static ArrayList
                            localSubjects.add(subject);
                            Log.d("SubjectFetch", "Subject: " + subject.getSubjectName()
                                    + ", Status: " + subject.getStatus()
                                    + ", Semester: " + subject.getSemester()
                                    + ", ID: " + subject.subjectId);
                        }
                        localSubjects.clear();
                        localSubjects.addAll(allSubjects);
                        listener.onSuccess(localSubjects);
                    }
                    listener.onSuccess(allSubjects);
                })
                .addOnFailureListener(e -> {
                    Log.e("SubjectFetch", "Error fetching subjects", e);
                    listener.onFailure("Failed to fetch subjects: " + e.getMessage());
                });
    }



    public interface OnAllSubjectsFetchedListener {
        void onSuccess(List<Subjects.Subject> subjects);

        void onFailure(String errorMessage);
    }


    public interface OnActiveSubjectsFetchedListener {
        void onSuccess(List<Subjects.Subject> activeSubjects);

        void onFailure(String errorMessage);
    }


    // Interface for the listener (unchanged)

    public void deleteSubject(Subjects.Subject subject, OnSubjectDeletedListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("subjects")
                .whereEqualTo("subjectName", subject.getSubjectName())
                //.whereEqualTo("userId", userId)
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
            listener.onFailure("User not signed in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Tasks.Task> tasks = new ArrayList<>();
                    List<Tasks.Task> tasksToUpdate = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Tasks.Task task = document.toObject(Tasks.Task.class);
                        if (task != null) {
                            task.setTaskId(document.getId());
                            tasksToUpdate.add(task);
                            tasks.add(task);
                        }
                    }

                    // Fetch project and subject details for all tasks
                    fetchProjectAndSubjectDetails(tasksToUpdate, () -> {
                        listener.onSuccess(tasks);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("TaskFetch", "Error fetching tasks", e);
                    listener.onFailure("Failed to fetch tasks: " + e.getMessage());
                });
    }

    private void fetchProjectAndSubjectDetails(List<Tasks.Task> tasks, Runnable onComplete) {
        AtomicInteger counter = new AtomicInteger(tasks.size() * 2); // 2 operations per task

        for (Tasks.Task task : tasks) {
            // Handle project reference
            if (task.getTaskProject() != null) {
                task.getTaskProject().get().addOnSuccessListener(projectSnapshot -> {
                    if (projectSnapshot.exists()) {
                        task.setTaskProjectString(projectSnapshot.getString("projectName"));
                    }
                    if (counter.decrementAndGet() == 0) {
                        onComplete.run();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("TaskFetch", "Error fetching project details", e);
                    if (counter.decrementAndGet() == 0) {
                        onComplete.run();
                    }
                });
            } else {
                if (counter.decrementAndGet() == 0) {
                    onComplete.run();
                }
            }

            // Handle subject reference
            if (task.getTaskSubject() != null) {
                task.getTaskSubject().get().addOnSuccessListener(subjectSnapshot -> {
                    if (subjectSnapshot.exists()) {
                        task.setTaskSubjectString(subjectSnapshot.getString("subjectName"));
                    }
                    if (counter.decrementAndGet() == 0) {
                        onComplete.run();
                    }
                }).addOnFailureListener(e -> {
                    Log.e("TaskFetch", "Error fetching subject details", e);
                    if (counter.decrementAndGet() == 0) {
                        onComplete.run();
                    }
                });
            } else {
                if (counter.decrementAndGet() == 0) {
                    onComplete.run();
                }
            }
        }
    }

    public interface OnTasksFetchedListener {
        void onSuccess(List<Tasks.Task> tasks);

        void onFailure(String errorMessage);
    }
}