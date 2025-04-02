package com.example.listi.ui.authentication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.listi.MainActivity;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentHomeBinding;
import com.example.listi.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private Spinner yearGroupSpinner;
    private ArrayAdapter<String> adapter;
    private FragmentProfileBinding binding;
    private UserViewModel userViewModel;
    private FirebaseAuth auth;

    private FirebaseUser user;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();


        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.displayName.setText(user.getDisplayName());

            } else {
                binding.displayName.setText("Not signed in");

            }
        });
        binding.logout.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).signOut();
            FirebaseAuth.getInstance().signOut();
            Navigation.findNavController(requireView()).navigate(R.id.action_profileFragment_to_homeFragment);

        });





        return root;



    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
