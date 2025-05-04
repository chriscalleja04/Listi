package com.example.listi.ui.authentication;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import static android.content.ContentValues.TAG;

import com.example.listi.AuthViewModel;
import com.example.listi.MainActivity;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentAuthenticationBinding;
import com.example.listi.databinding.FragmentRegistrationBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegistrationFragment extends Fragment {

    private FirebaseFirestore db;
    private FragmentRegistrationBinding binding;
    private UserViewModel userViewModel;
    private AuthViewModel authViewModel;

    private FirebaseAuth mAuth;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Navigation.findNavController(requireView()).navigate(R.id.action_authenticateFragment_to_registrationFragment);
        }
    }


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        authViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            binding.progressBar.setVisibility(View.GONE);
            if(user != null){
                Navigation.findNavController(requireView()).navigate(R.id.action_registrationFragment_to_homeFragment);
            }
        });

        authViewModel.getAuthMessage().observe(getViewLifecycleOwner(), message -> {
            if(message != null && !message.isEmpty()){
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });


        binding.registerButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
              binding.progressBar.setVisibility(View.VISIBLE);
              String email, password, displayName;
              email = String.valueOf(binding.email.getText());
              password = String.valueOf(binding.password.getText());
              displayName = String.valueOf(binding.displayName.getText());

              if (TextUtils.isEmpty(email)) {
                  Toast.makeText(requireContext(), "Daħħal Email", Toast.LENGTH_SHORT).show();
                  return;
              }
              if (TextUtils.isEmpty(password)) {
                  Toast.makeText(requireContext(), "Daħħal Password", Toast.LENGTH_SHORT).show();
                  return;
              }
              if (TextUtils.isEmpty(displayName)) {
                  Toast.makeText(requireContext(), "Daħħal Isem", Toast.LENGTH_SHORT).show();
                  return;
              }

              authViewModel.registerWithEmailPassword(email, password, displayName);
          }
      });
        binding.loginNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(requireView()).navigate(R.id.action_registrationFragment_to_authenticateFragment);

            }
        });

        binding.loginMicrosoft.setOnClickListener(v -> {
            authViewModel.loginWithMicrosoft(requireActivity());

        });

        return root;

        }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(authViewModel != null){
            authViewModel.clearAuthMessage();
        }
        binding = null;
    }
}