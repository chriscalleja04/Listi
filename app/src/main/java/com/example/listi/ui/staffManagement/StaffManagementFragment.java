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
import com.example.listi.YearClassRepository;
import com.example.listi.databinding.FragmentStaffManagementBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
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
    private Spinner yearGroupSpinner, classRoomSpinner;
    private ArrayAdapter<String> adapterYearGroup, adapterClassRoom;

    private FragmentStaffManagementBinding binding;
    private UserViewModel userViewModel;

    private YearClassRepository yearClassRepository;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        binding = FragmentStaffManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        yearClassRepository = new YearClassRepository(userViewModel);

        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                userViewModel.getSchoolName().observe(getViewLifecycleOwner(), schoolName -> {
                    Log.d(TAG, "School name observed in fragment: " + schoolName);
                    binding.schoolName.setText(schoolName);
                });
                yearGroupSpinner = binding.yearGroup;
                adapterYearGroup = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>()
                );
                adapterYearGroup.setDropDownViewResource(android.R.layout.simple_spinner_item);
                yearGroupSpinner.setAdapter(adapterYearGroup);
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

                classRoomSpinner = binding.classSpinner;
                adapterClassRoom = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>()
                );
                adapterClassRoom.setDropDownViewResource(android.R.layout.simple_spinner_item);
                classRoomSpinner.setAdapter(adapterClassRoom);
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
                binding.schoolName.setText("Not Signed in");
                binding.yearGroup.setAdapter(null);
                binding.classSpinner.setAdapter(null);
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
                    String selectedClass = classRoomSpinner.getSelectedItem().toString(); // Get selected class name
                    yearClassRepository.saveNewEducator(name, email, selectedClass,
                            unused -> {
                                Toast.makeText(requireContext(), "Suċċess! Ħloqt Edukatur bla problemi", Toast.LENGTH_SHORT).show();
                                clearFields();
                            },
                            e -> {
                                Toast.makeText(requireContext(), "Kien hemm problema" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error adding educator", e);
                            }
                    );
                }
            }
        });


        return root;
    }
    private void clearFields() {
        binding.educatorName.setText("");
        binding.educatorName.clearFocus();
        binding.educatorEmail.setText("");
        binding.educatorEmail.clearFocus();
        binding.yearGroup.setSelection(0);
        binding.classSpinner.setSelection(0);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}