package com.example.learnleaf;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;

public class Navigation extends Fragment {

    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation, container, false);

        mAuth = FirebaseAuth.getInstance();

        TextView tasksNav = view.findViewById(R.id.nav_tasks);
        TextView subjectsNav = view.findViewById(R.id.nav_subjects);
        TextView projectsNav = view.findViewById(R.id.nav_projects);
        TextView profileNav = view.findViewById(R.id.nav_profile);
        TextView logoutNav = view.findViewById(R.id.nav_logout);
        //TextView archivesNav = view.findViewById(R.id.nav_archives);

        tasksNav.setOnClickListener(v -> navigateTo(Tasks.class));
        subjectsNav.setOnClickListener(v -> navigateTo(Subjects.class));
        projectsNav.setOnClickListener(v -> navigateTo(Projects.class));
        profileNav.setOnClickListener(v -> navigateTo(Profile.class));
        //archivesNav.setOnClickListener(v -> navigateTo(Archives.class));
        logoutNav.setOnClickListener(v -> logout());

        return view;
    }

    private void navigateTo(Class<?> destinationClass) {
        Intent intent = new Intent(getActivity(), destinationClass);
        startActivity(intent);
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}