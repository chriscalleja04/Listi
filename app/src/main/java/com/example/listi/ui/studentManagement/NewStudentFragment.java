package com.example.listi.ui.studentManagement;

import static android.content.ContentValues.TAG;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.YearClassRepository;
import com.example.listi.databinding.FragmentNewStudentBinding;
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
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewStudentFragment extends Fragment {
    private FirebaseFirestore db;
    private Spinner yearGroupSpinner, classRoomSpinner;
    private ArrayAdapter<String> adapterYearGroup, adapterClassRoom;

    private FragmentNewStudentBinding binding;
    private UserViewModel userViewModel;
    private YearClassRepository yearClassRepository;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        binding = FragmentNewStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        yearClassRepository = new YearClassRepository(userViewModel);

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
                        yearClassRepository.fetchYearGroupID(selectedYearGroup, getViewLifecycleOwner());
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



    // Modified saveNewStudent to accept selectedClassRoomName
    public void saveNewStudent(String name, String email, String selectedClassRoomName) {
        yearClassRepository.fetchClassRoomIDByName(selectedClassRoomName).addOnCompleteListener(task -> { // Fetch ClassRoomID here based on name
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
                                Toast.makeText(requireContext(), "Suċċess! L-Istudent inħalaq bla problemi", Toast.LENGTH_SHORT).show();
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