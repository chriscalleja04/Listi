package com.example.listi.ui.staffManagement;

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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffManagementFragment extends Fragment {

    private FirebaseFirestore db;
    private Spinner yearGroupSpinner;
    private ArrayAdapter<String> adapter;
    private com.example.listi.databinding.FragmentStaffManagementBinding binding;
    private UserViewModel userViewModel;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        binding = FragmentStaffManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null ) {
                userViewModel.getSchoolName().observe(getViewLifecycleOwner(), schoolName -> {
                    binding.schoolName.setText(schoolName);
                });
                yearGroupSpinner = binding.yearGroup;
                adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>()
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
                yearGroupSpinner.setAdapter(adapter);
                userViewModel.getYearGroups().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                    @Override
                    public void onChanged(List<String> yearGroups) {
                        if (yearGroups != null) {
                            adapter.clear();
                            adapter.addAll(yearGroups);
                            adapter.notifyDataSetChanged();
                        }
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

            } else {
                binding.schoolName.setText("Not Signed in");
                binding.yearGroup.setAdapter(null);
            }
        });
        binding.addEducator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = binding.educatorName.getText().toString().trim();
                String email = binding.educatorEmail.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please Enter a Name to Submit", Toast.LENGTH_SHORT).show();
                } else if (email.isEmpty()) {
                    Toast.makeText(requireContext(), "Please Enter an Email to Submit", Toast.LENGTH_SHORT).show();
                } else {
                    saveNewEducator(name, email);
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
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String yearGroupID = document.getId();
                                userViewModel.setYearGroupID(yearGroupID);
                                Log.d("Firestore", "Year Group ID: " + yearGroupID);
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }



    public void saveNewEducator(String name, String email){
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupID = userViewModel.getYearGroupID().getValue();
        Map<String, Object> educatorDetails = new HashMap<>();
        educatorDetails.put("name",name);
        educatorDetails.put("email",email);
        assert schoolID != null;
        assert yearGroupID != null;
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupID)
                .collection("educators")
                .add(educatorDetails)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(requireContext(),"Success, Educator Added Successfully!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                        binding.educatorName.setText("");
                        binding.educatorName.clearFocus();
                        binding.educatorEmail.setText("");
                        binding.educatorEmail.clearFocus();
                        binding.yearGroup.setSelection(0);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Error Writing document", e);
                    }
                });




    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
