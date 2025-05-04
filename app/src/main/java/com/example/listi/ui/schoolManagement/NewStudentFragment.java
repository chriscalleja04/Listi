package com.example.listi.ui.schoolManagement;

import static android.content.ContentValues.TAG;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.listi.ClassRoom;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.YearClassRepository;
import com.example.listi.YearGroup;
import com.example.listi.databinding.FragmentNewStudentBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NewStudentFragment extends Fragment {
    private FirebaseFirestore db;
    private Spinner yearGroupSpinner, classRoomSpinner;
    private ArrayAdapter<String> adapterYearGroup, adapterClassRoom;
    private FragmentNewStudentBinding binding;
    private UserViewModel userViewModel;
    private YearClassRepository yearClassRepository;

    private String yearGroupId, classRoomId;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        binding = FragmentNewStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        yearClassRepository = new YearClassRepository(userViewModel);

        if (getArguments() != null) {
            yearGroupId = getArguments().getString("yearGroupId");
            classRoomId = getArguments().getString("classRoomId");

            userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    userViewModel.getSchoolName().observe(getViewLifecycleOwner(), schoolName -> {
                        binding.schoolNameStudent.setText(schoolName);
                    });

                    binding.addStudent.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String name = binding.studentName.getText().toString().trim();
                            String email = binding.studentEmail.getText().toString().trim();
                            if (name.isEmpty()) {
                                Toast.makeText(requireContext(), "Daħħal isem biex tkompli", Toast.LENGTH_SHORT).show();
                            } else if (email.isEmpty()) {
                                Toast.makeText(requireContext(), "Daħħal email biex tkompli", Toast.LENGTH_SHORT).show();
                            } else {
                                yearClassRepository.saveNewStudent(yearGroupId, classRoomId, name, email,
                                    unused -> {
                                        Toast.makeText(requireContext(), "Suċċess! Ħloqt Student bla problemi", Toast.LENGTH_SHORT).show();
                                        clearFields();
                                        Navigation.findNavController(requireView()).popBackStack();
                                    },
                                    e -> {
                                        Toast.makeText(requireContext(), "Kien hemm problema" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Error adding student", e);
                                    }
                                );
                            }
                        }
                    });

                }
            });

        }
        return root;
    }

    private void clearFields() {
        binding.studentName.setText("");
        binding.studentName.clearFocus();
        binding.studentEmail.setText("");
        binding.studentEmail.clearFocus();
     /*   binding.yearGroupEducator.setSelection(0);
        binding.classSpinnerEducator.setSelection(0);*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}