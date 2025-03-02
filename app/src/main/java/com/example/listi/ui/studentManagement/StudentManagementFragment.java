package com.example.listi.ui.studentManagement;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentStaffManagementBinding;
import com.example.listi.databinding.FragmentStudentManagementBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentManagementFragment extends Fragment {

    private FirebaseFirestore db;
    private Spinner yearGroupSpinner, classRoomSpinner;
    private ArrayAdapter<String> adapterYearGroup, adapterClassRoom;

    private FragmentStudentManagementBinding binding;
    private UserViewModel userViewModel;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        binding = FragmentStudentManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                userViewModel.getSchoolName().observe(getViewLifecycleOwner(), schoolName -> {
                    binding.schoolNameStudent.setText(schoolName);
                });

                yearGroupSpinner = binding.yearGroupStudent;
                adapterYearGroup = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>()
                );
                adapterYearGroup.setDropDownViewResource(android.R.layout.simple_spinner_item);
                yearGroupSpinner.setAdapter(adapterYearGroup);

                userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                    if (role.equals("admin")) {
                        userViewModel.getYearGroups().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> yearGroups) {
                                if (yearGroups != null) {
                                    adapterYearGroup.clear();
                                    adapterYearGroup.addAll(yearGroups);
                                    adapterYearGroup.notifyDataSetChanged();
                                }
                            }
                        });
                    } else if (role.equals("educator")) {
                        userViewModel.getYearGroupsEducator().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> yearGroups) {
                                if (yearGroups != null) {
                                    adapterYearGroup.clear();
                                    adapterYearGroup.addAll(yearGroups);
                                    adapterYearGroup.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                });


                classRoomSpinner = binding.classSpinnerStudent;
                adapterClassRoom = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>()
                );
                adapterClassRoom.setDropDownViewResource(android.R.layout.simple_spinner_item);
                classRoomSpinner.setAdapter(adapterClassRoom);


                userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                    if (role.equals("admin")) {
                        userViewModel.getClasses().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> classes) {
                                if (classes != null) {
                                    adapterClassRoom.clear();
                                    adapterClassRoom.addAll(classes);
                                    adapterClassRoom.notifyDataSetChanged();
                                }
                            }
                        });
                    } else if (role.equals("educator")) {
                        userViewModel.getClassesEducator().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> classes) {
                                if (classes != null) {
                                    adapterClassRoom.clear();
                                    adapterClassRoom.addAll(classes);
                                    adapterClassRoom.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                });

                yearGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        String selectedYearGroup = adapterView.getItemAtPosition(i).toString();
                        fetchYearGroupID(selectedYearGroup);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                classRoomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });


            } else {
                binding.schoolNameStudent.setText("Not Signed in");
                binding.yearGroupStudent.setAdapter(null);
                binding.classSpinnerStudent.setAdapter(null);
            }
        });
        binding.addStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = binding.studentName.getText().toString().trim();
                String email = binding.studentEmail.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please Enter a Name to Submit", Toast.LENGTH_SHORT).show();
                } else if (email.isEmpty()) {
                    Toast.makeText(requireContext(), "Please Enter an Email to Submit", Toast.LENGTH_SHORT).show();
                } else {
                    String selectedClass = classRoomSpinner.getSelectedItem().toString(); // Get selected class name
                    saveNewStudent(name, email, selectedClass); // Pass selected class name to saveNewStudent
                }
            }
        });


        return root;
    }


    public void fetchYearGroupID(String selectedYearGroup) {
        String schoolID = userViewModel.getSchoolID().getValue();
        CollectionReference schoolsRef = db.collection("schools");
        assert schoolID != null;
        DocumentReference schoolIDRef = schoolsRef.document(schoolID);
        CollectionReference yearGroupsRef = schoolIDRef.collection("yearGroups");
        yearGroupsRef.whereEqualTo("name", selectedYearGroup)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                String yearGroupID = querySnapshot.getDocuments().get(0).getId();
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                assert currentUser != null;
                                String email = currentUser.getEmail();
                                userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                                    fetchClassRooms(yearGroupID, role, email);
                                });
                                userViewModel.setYearGroupID(yearGroupID);
                                Log.d("Firestore", "Year Group ID: " + yearGroupID);

                            } else {
                                Log.d(TAG, "Doc does not exist");

                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public Task<String> fetchClassRoomIDByName(String selectedClassRoom) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupID = userViewModel.getYearGroupID().getValue();
        assert schoolID != null;
        assert yearGroupID != null;
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupID)
                .collection("classes")
                .whereEqualTo("name", selectedClassRoom)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                String classRoomID = querySnapshot.getDocuments().get(0).getId();
                                taskCompletionSource.setResult(classRoomID);
                            } else {
                                Log.d(TAG, "Name does not exist");
                                taskCompletionSource.setException(new Exception("Classroom name does not exist")); // Or handle no class found differently
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            taskCompletionSource.setException(task.getException());
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }


    public void fetchClassRooms(String yearGroupId, String role, String email) {
        String schoolID = userViewModel.getSchoolID().getValue();
        assert schoolID != null;
        if (role.equals("admin")) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<String> classes = new ArrayList<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String name = document.getString("name");
                                    if (name != null) {
                                        classes.add(name);
                                    }
                                }
                                userViewModel.setClasses(classes);
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        } else {
            if (role.equals("educator")) {
                fetchEducatorClasses(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> classIds = task.getResult();
                        if (classIds != null && !classIds.isEmpty()) {
                            db.collection("schools")
                                    .document(schoolID)
                                    .collection("yearGroups")
                                    .document(yearGroupId)
                                    .collection("classes")
                                    .whereIn(FieldPath.documentId(), classIds)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                List<String> classRoomNames = new ArrayList<>();
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    String name = document.getString("name");
                                                    if (name != null) {
                                                        classRoomNames.add(name);
                                                    }
                                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                                }
                                                userViewModel.setClassesEducator(classRoomNames);
                                            } else {
                                                Log.d(TAG, "Error getting documents: ", task.getException());
                                            }
                                        }
                                    });
                        }
                    }
                });
            }
        }
    }

    public Task<List<String>> fetchEducatorClasses(String email) {
        TaskCompletionSource<List<String>> taskCompletionSource = new TaskCompletionSource<>();
        List<String> result = new ArrayList<>();

        db.collectionGroup("educators")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(educatorTask -> {
                    if (educatorTask.isSuccessful()) {
                        QuerySnapshot querySnapshot = educatorTask.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot doc : educatorTask.getResult()) {
                                String classId = doc.getString("classRoomId");
                                if (classId != null) {
                                    result.add(classId);
                                }
                                Log.d(TAG, "Found educator in path: " + doc.getReference().getPath());
                            }
                            taskCompletionSource.setResult(result);
                        } else {
                            // No documents found
                            Log.d(TAG, "No educators found for email: " + email);
                            taskCompletionSource.setResult(result); // Return empty list
                        }
                    } else {
                        Log.e(TAG, "Error in educator query", educatorTask.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    // Modified saveNewStudent to accept selectedClassRoomName
    public void saveNewStudent(String name, String email, String selectedClassRoomName) {
        fetchClassRoomIDByName(selectedClassRoomName).addOnCompleteListener(task -> { // Fetch ClassRoomID here based on name
            if (task.isSuccessful()) {
                String classRoomID = task.getResult();
                String schoolID = userViewModel.getSchoolID().getValue();
                String yearGroupID = userViewModel.getYearGroupID().getValue();
                String role = "student";
                Map<String, Object> educatorDetails = new HashMap<>();
                educatorDetails.put("name", name);
                educatorDetails.put("email", email);
                educatorDetails.put("role", role);
                educatorDetails.put("schoolId", schoolID);
                educatorDetails.put("yearGroupId", yearGroupID);
                educatorDetails.put("classId", classRoomID);
                assert schoolID != null;
                assert yearGroupID != null;
                db.collection("schools")
                        .document(schoolID)
                        .collection("yearGroups")
                        .document(yearGroupID)
                        .collection("classes")
                        .document(classRoomID)
                        .collection("students")
                        .add(educatorDetails)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Toast.makeText(requireContext(), "Success, Student Added Successfully!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "DocumentSnapshot successfully written!");
                                binding.studentName.setText("");
                                binding.studentName.clearFocus();
                                binding.studentEmail.setText("");
                                binding.studentEmail.clearFocus();
                                binding.yearGroupStudent.setSelection(0);
                                binding.classSpinnerStudent.setSelection(0);
                                db.collection("users")
                                        .whereEqualTo("email", email)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    QuerySnapshot querySnapshot = task.getResult();
                                                    if (!querySnapshot.isEmpty()) {
                                                        String userId = querySnapshot.getDocuments().get(0).getId();
                                                        Map<String, Object> userDetails = new HashMap<>();
                                                        userDetails.put("email", email);
                                                        userDetails.put("name", name);
                                                        userDetails.put("role", role);
                                                        userDetails.put("schoolId", userViewModel.getSchoolID().getValue());
                                                        db.collection("users")
                                                                .document(userId)
                                                                .set(userDetails)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void unused) {
                                                                        Log.d(TAG, "DocumentSnapshot successfully written!");

                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.w(TAG, "Error writing document", e);

                                                                    }
                                                                });

                                                    } else {
                                                        Log.d(TAG, "User does not exist, not added to uses collection");
                                                    }

                                                } else {
                                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                                }
                                            }
                                        });
                            }

                        }).addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Error adding student. Could not fetch class ID.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error fetching ClassRoomID: ", e);
                        });


            } else {
                Toast.makeText(requireContext(), "Could not fetch Class ID. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });


    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}