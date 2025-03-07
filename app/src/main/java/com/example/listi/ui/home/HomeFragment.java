package com.example.listi.ui.home;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.listi.MainActivity;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {
    private FirebaseFirestore db;
    private Spinner yearGroupSpinner;
    private ArrayAdapter<String> adapter;
    private FragmentHomeBinding binding;
    private UserViewModel userViewModel;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null ) {
                binding.displayName.setText(user.getDisplayName());

            } else {
                binding.displayName.setText("Not signed in");

            }
        });
        binding.loginButton.setOnClickListener(v ->
                ((MainActivity)requireActivity()).startSignInFlow()
        );

        binding.logout.setOnClickListener(v ->
                ((MainActivity)requireActivity()).signOut()
        );




        return root;



    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
