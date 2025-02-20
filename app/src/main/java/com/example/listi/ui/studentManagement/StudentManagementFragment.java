package com.example.listi.ui.studentManagement;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentStudentManagementBinding;

public class StudentManagementFragment extends Fragment {

    private StudentManagementViewModel mViewModel;
    private FragmentStudentManagementBinding binding;
    private UserViewModel userViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentStudentManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        return root;
    };
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}