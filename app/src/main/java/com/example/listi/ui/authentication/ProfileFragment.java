package com.example.listi.ui.authentication;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.listi.MainActivity;
import com.example.listi.MyHorizontalAdapter;
import com.example.listi.Profile;
import com.example.listi.R;
import com.example.listi.RecyclerViewInterface;
import com.example.listi.UserViewModel;
import com.example.listi.WordList;
import com.example.listi.databinding.FragmentHomeBinding;
import com.example.listi.databinding.FragmentProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;

public class ProfileFragment extends Fragment implements RecyclerViewInterface {

    private FirebaseFirestore db;
    private Spinner yearGroupSpinner;
    private ArrayAdapter<String> adapter;
    private FragmentProfileBinding binding;
    private UserViewModel userViewModel;
    private FirebaseAuth auth;

    private FirebaseUser user;

    private RecyclerView rv;
    private ArrayList<Profile> profilesList;

    private LinearLayoutManager linearLayoutManager;

    private MyHorizontalAdapter myHorizontalAdapter;
    Profile profile = new Profile();


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();


        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.horizontalRv.setLayoutManager(linearLayoutManager);

        profilesList = new ArrayList<>();
        myHorizontalAdapter = new MyHorizontalAdapter(requireContext(), profilesList, this);

        binding.horizontalRv.setAdapter(myHorizontalAdapter);

        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);


        userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
            myHorizontalAdapter.setUserRole(role);


            userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
                if (user != null) {
                    profilesList.clear();
                    profile.setName(user.getDisplayName());
                    profile.setID(user.getUid());
                    profilesList.add(profile);
                    if (role.equals("public")) {
                        db.collection("users")
                                .document(user.getUid())
                                .collection("childProfiles")
                                .orderBy("name", Query.Direction.ASCENDING)
                                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                        if (error != null) {
                                            // Handle Firestore errors
                                            Log.e("Firestore error", error.getMessage());
                                            return;
                                        }

                                        // Check if the QuerySnapshot is null or empty
                                        if (value == null || value.isEmpty()) {
                                            Log.d(TAG, "No documents found in the query.");
                                            return;
                                        }

                                        // Process document changes
                                        for (DocumentChange document : value.getDocumentChanges()) {
                                            if (document.getType() == DocumentChange.Type.ADDED) {
                                                Profile profile = document.getDocument().toObject(Profile.class);
                                                profile.setID(document.getDocument().getId());
                                                profilesList.add(profile);
                                            }
                                        }
                                        // Notify the adapter of data changes
                                        myHorizontalAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                } else {
                    profilesList.clear();
                    profile.setName("Not signed in");
                    profilesList.add(profile);

                }
            });
        });


        myHorizontalAdapter.setOnItemActionListener(new MyHorizontalAdapter.OnItemActionListener() {
            @Override
            public void onAddChildClick() {
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_profileFragment_to_createChildFragment);
            }

           @Override
            public void onLogoutClick() {
                userViewModel.setChildID(null);
                userViewModel.setChildName(null);
                SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();
                ((MainActivity) requireActivity()).signOut();
                FirebaseAuth.getInstance().signOut();
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_profileFragment_to_homeFragment);
            }
        });


        return root;

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemClick(int position) {
        Profile profile = profilesList.get(position);
        String clickedProfileId = profile.getID();
        String clickedProfileName = profile.getName();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Get current role directly (no observation needed here)
        String currentRole = userViewModel.getRole().getValue();
        if (currentRole == null || !currentRole.equals("public")) return;

        if (currentUser == null) return;

        // Block clicks on parent's own profile
        if (clickedProfileId.equals(currentUser.getUid())) {
            Log.d(TAG, "Parent clicked their own profile - blocking");
            return;
        }

        // Block clicks if already viewing as a child
        String currentChildId = userViewModel.getChildID().getValue();
        if (currentChildId != null && !currentChildId.isEmpty()) {
            Log.d(TAG, "Created profile tried to click - blocking");
            return;
        }

        // Update state
        userViewModel.setChildID(clickedProfileId);
        userViewModel.setChildName(clickedProfileName);

        ((MainActivity) requireActivity()).updateUserData(
                FirebaseAuth.getInstance().getCurrentUser());

        // Update preferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("child_id", clickedProfileId)
                .putString("child_name", clickedProfileName)
                .apply();

        // Navigate
        Navigation.findNavController(requireView()).navigate(R.id.action_profileFragment_to_homeFragment);
    }
}

