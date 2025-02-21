package learn.leaf.learnleaf;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.PropertyName;
import java.lang.reflect.Field;
import java.util.List;

public class Subjects extends AppCompatActivity {
    private LinearLayout subjectsContainer;
    private Firebase firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subjects);

        subjectsContainer = findViewById(R.id.subjectsContainer);
        ImageView addnew = findViewById(R.id.addnew);
        firebase = new Firebase(this);
        addnew.setOnClickListener(v -> showCreateSubjectDialog());
        fetchAllSubjectsForCurrentUser();
    }

    //Initiated by pressing addNew button, Allows user to create a new Subject
    private void showCreateSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Subject");

        //opens dialog_create_subject
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_create_subject, null);
        final EditText subjectNameInput = viewInflated.findViewById(R.id.subjectNameInput);
        final EditText semesterInput = viewInflated.findViewById(R.id.semesterInput);
        final EditText descriptionInput = viewInflated.findViewById(R.id.subjectDescriptionInput);
        Button colorPicker = viewInflated.findViewById(R.id.colorPickerButton);

        updateColorButton(colorPicker);
        colorPicker.setOnClickListener(v -> showColorPickerDialog(colorPicker));

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, null); // We'll set the listener later
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false); // Initially disable the button

            //This will not allow a user to submit new Subject if subject name is blank
            subjectNameInput.addTextChangedListener(new TextWatcher() {
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
                String subjectName = subjectNameInput.getText().toString().trim();
                String semester = semesterInput.getText().toString().trim();
                String color = String.format("#%06X", (0xFFFFFF & selectedColor));
                String description = descriptionInput.getText().toString().trim();
                createNewSubject(subjectName, semester, color, description);
                dialog.dismiss();
            });
        });

        dialog.show();
    }



    private int selectedColor = Color.BLACK; // Default color, define this as a class member

    private void showEditSubjectDialog(Subject subject) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Subject");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_edit_subject, null);
        final EditText subjectNameInput = viewInflated.findViewById(R.id.subjectNameInput);
        final EditText semesterInput = viewInflated.findViewById(R.id.semesterInput);
        final EditText descriptionInput = viewInflated.findViewById(R.id.subjectDescriptionInput);
        final Spinner statusSpinner = viewInflated.findViewById(R.id.statusSpinner);
        final Button colorPickerButton = viewInflated.findViewById(R.id.colorPickerButton);

        subjectNameInput.setText(subject.getSubjectName());
        semesterInput.setText(subject.getSubjectSemester());
        selectedColor = Color.parseColor(subject.getSubjectColor()); // Parse the color from the subject

        // Set up the color picker button
        updateColorButton(colorPickerButton);
        colorPickerButton.setOnClickListener(v -> showColorPickerDialog(colorPickerButton));

        // Sets up the status spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(adapter);
        int spinnerPosition = adapter.getPosition(subject.getSubjectStatus());
        statusSpinner.setSelection(spinnerPosition);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String subjectName = subjectNameInput.getText().toString();
            String semester = semesterInput.getText().toString();
            String color = String.format("#%06X", (0xFFFFFF & selectedColor)); // Convert color to hex string
            String status = statusSpinner.getSelectedItem().toString();
            String description = descriptionInput.getText().toString();
            updateSubject(subject, subjectName, semester, color, status, description);
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showColorPickerDialog(Button colorPickerButton) {
        int[] COLORS = {
                Color.parseColor("#1976D2"), // Blue
                Color.parseColor("#388E3C"), // Green
                Color.parseColor("#D32F2F"), // Red
                Color.parseColor("#7B1FA2"), // Purple
                Color.parseColor("#FFA000"), // Amber
                Color.parseColor("#00796B"), // Teal
                Color.parseColor("#C2185B"), // Pink
                Color.parseColor("#0097A7"), // Cyan
                Color.parseColor("#689F38"), // Light Green
                Color.parseColor("#303F9F"), // Indigo
                Color.parseColor("#455A64"), // Blue Grey
                Color.parseColor("#F57C00")  // Orange
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(4); // 4 columns
        gridLayout.setPadding(16, 16, 16, 16);

        // Create the dialog first
        AlertDialog dialog = builder.create();
        dialog.setTitle("Choose a Color");

        for (int color : COLORS) {
            Button colorButton = new Button(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 100;
            params.height = 100;
            params.setMargins(8, 8, 8, 8);
            colorButton.setLayoutParams(params);
            colorButton.setBackgroundColor(color);

            colorButton.setOnClickListener(v -> {
                selectedColor = color;
                updateColorButton(colorPickerButton);
                dialog.dismiss(); // Explicitly dismiss the dialog
            });

            gridLayout.addView(colorButton);
        }

        // Set the view after creating the dialog
        dialog.setView(gridLayout);
        dialog.show();
    }

    private void updateColorButton(Button colorPickerButton) {
        colorPickerButton.setBackgroundColor(selectedColor);
        colorPickerButton.setTextColor(getContrastColor(selectedColor));
    }

    private int getContrastColor(int color) {
        double y = (299 * Color.red(color) + 587 * Color.green(color) + 114 * Color.blue(color)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    //Call to method in Firebase.java
    private void updateSubject(Subject subject, String subjectName, String semester, String color, String status, String description) {
        firebase.updateSubject(subject, subjectName, semester, color, status, description, new Firebase.OnSubjectUpdatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Subjects.this, "Subject updated successfully", Toast.LENGTH_SHORT).show();
                fetchAllSubjectsForCurrentUser(); // Refresh the list
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Call to method in Firebase.java
    private void createNewSubject(String subjectName, String semester, String color, String description) {
        firebase.createNewSubject(subjectName, semester, color, description, new Firebase.OnSubjectCreatedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(Subjects.this, "Subject created successfully", Toast.LENGTH_SHORT).show();
                fetchAllSubjectsForCurrentUser(); // Refresh the list
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showDeleteConfirmationDialog(Subject subject, View blockView) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        builder.setMessage("What action would you like to take?");

        builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Simply dismiss the dialog
                dialog.dismiss();
            }
        });

        // Delete and Block button
        builder.setNegativeButton("Delete and Block", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Perform delete and block action
                performDeleteAndBlock(subject, blockView);
            }
        });

        // Delete button
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performDelete(subject, blockView);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void performDelete(Subject subject, View blockView) {
        firebase.deleteSubject(subject, new Firebase.OnSubjectDeletedListener() {
            @Override
            public void onSuccess() {
                subjectsContainer.removeView(blockView);
                Toast.makeText(Subjects.this, "Subject deleted successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performDeleteAndBlock(Subject subject, View blockView) {
        subject.setSubjectStatus("Blocked");
        updateSubject(subject, subject.getSubjectName(), subject.getSubjectSemester(),
                subject.getSubjectColor(), subject.getSubjectStatus(), subject.getSubjectDescription());
        fetchAllSubjectsForCurrentUser();
    }


    //Call to method in Firebase.java
    private void fetchAllSubjectsForCurrentUser() {
        firebase.fetchAllSubjectsForCurrentUser(new Firebase.OnAllSubjectsFetchedListener() {
            @Override
            public void onSuccess(List<Subject> allSubjects) {
                updateUI(allSubjects);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Subjects.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //After fetching new list of Subjects, this method will update the item_blocks in the Scrollview
    public void updateUI(List<Subject> subjects) {
        if (subjectsContainer == null) {
            Toast.makeText(Subjects.this, "Null container", Toast.LENGTH_SHORT).show();
            return;
        }

        subjectsContainer.removeAllViews();

        if (subjects.isEmpty()) {
            TextView noSubjectsText = new TextView(this);
            noSubjectsText.setText("No subjects found");
            subjectsContainer.addView(noSubjectsText);
            return;
        }

        for (Subject subject : subjects) {
            //creates item block for each subject in subject list
            Log.d("UpdateUI", "Updating UI with " + subjects.size() + " subjects");
            View blockView = getLayoutInflater().inflate(R.layout.item_block, subjectsContainer, false);

            CardView cardView = blockView.findViewById(R.id.itemCardView);
            TextView nameTextView = blockView.findViewById(R.id.itemNameTextView);
            TextView statusTextView = blockView.findViewById(R.id.itemStatusTextView);
            TextView extraTextView = blockView.findViewById(R.id.itemExtraTextView);
            ImageButton editButton = blockView.findViewById(R.id.editButton);
            ImageButton deleteButton = blockView.findViewById(R.id.deleteButton);

            //name
            nameTextView.setText(subject.getSubjectName());
            //semester
            statusTextView.setText("Semester: " + subject.getSubjectSemester());
            //description
            extraTextView.setText("Semester: " + subject.getSubjectDescription());

            // Debug log to check the color value
            Log.d("UpdateUI", "Subject: " + subject.getSubjectName() + ", Color: " + subject.getSubjectColor());

            int color = parseColor(subject.getSubjectColor());
            nameTextView.setTextColor(color);

            String contentDescription = String.format("Subject: %s, Status: %s, Semester: %s",
                    subject.getSubjectName(), subject.getSubjectStatus(), subject.getSubjectSemester());
            cardView.setContentDescription(contentDescription);

            editButton.setOnClickListener(v -> showEditSubjectDialog(subject));
            deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(subject, blockView));

            cardView.setOnClickListener(v -> {
                String subjectName = subject.getSubjectName();
                Intent intent = new Intent(Subjects.this, Tasks.class);
                intent.putExtra("FILTER_SUBJECT", subjectName);
                startActivity(intent);
            });
            subjectsContainer.addView(blockView);
        }
    }

    //Improved parseColor method
    private int parseColor(String colorString) {
        if (colorString == null || colorString.isEmpty()) {
            Log.w("UpdateUI", "Empty or null color string, using default");
            return Color.LTGRAY; // Default color
        }
        try {
            if (colorString.startsWith("#")) {
                return Color.parseColor(colorString);
            } else {
                Field field = Color.class.getField(colorString.toUpperCase());
                return field.getInt(null);
            }
        } catch (Exception e) {
            Log.e("UpdateUI", "Error parsing color: " + colorString, e);
            return Color.LTGRAY; //Default color if color cannot be found
        }
    }



    // Subject class to represent the data model
    public static class Subject {

        private String subjectSemester;
        private String subjectStatus;
        private String subjectColor;
        private String subjectName;
        private String subjectDescription;
        private String subjectId;//Will have eventually


        //Empty constructor required by Firebase
        //Do not delete
        public Subject() {}

        public Subject(String semester, String status, String subjectColor, String subjectName) {
            this.subjectColor = subjectColor;
            this.subjectName = subjectName;
            this.subjectSemester = subjectSemester;
            this.subjectStatus = subjectStatus;
            this.subjectDescription = subjectDescription;
            this.subjectId = subjectId;
        }

        //Setters
        public void setSubjectColor(String color){this.subjectColor = color;}
        public void setSubjectName(String newSubjectName) {this.subjectName = newSubjectName;}
        public void setSubjectSemester(String newSubjectSemester){this.subjectSemester = newSubjectSemester;}
        public void setSubjectStatus(String newSubjectStatus){this.subjectStatus = newSubjectStatus;}
        public void setSubjectDescription(String newSubjectDescription){this.subjectDescription = newSubjectDescription;}
        public void setSubjectId(String newsubjectId){this.subjectId = newsubjectId;}


        // Getters
        public String getSubjectColor() {
            return subjectColor;
        }
        public String getSubjectName() {
            return subjectName;
        }
        public String getSubjectSemester(){return subjectSemester;}
        public String getSubjectStatus(){return subjectStatus;}
        public String getSubjectDescription(){return subjectDescription;}
        public String getSubjectId(){return subjectId;}

    }
}