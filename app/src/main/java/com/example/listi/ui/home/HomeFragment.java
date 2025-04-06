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
import androidx.navigation.Navigation;

import com.example.listi.MainActivity;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.WordList;
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
import com.google.gson.Gson;

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

    private Map<String, List<String>> wordsByList = new HashMap<>();



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        db = FirebaseFirestore.getInstance();

        //fetchSample();


        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.account.setText("Profil");
                binding.account.setOnClickListener(v -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_profileFragment);

                });

            } else {
                binding.account.setText("Idħol");
                binding.account.setOnClickListener(v -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_authenticateFragment);

                });
            }
        });

      /*  binding.sampleContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                Gson gson = new Gson();
                String json = gson.toJson(wordsByList);
                bundle.putString("wordsJson", json);

                Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_expandListFragment, bundle);

            }
        });*/







        return root;



    }
/*    private void fetchSample() {
        binding.progressBar2.setVisibility(View.VISIBLE);
        db.collection("sampleList")
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> words = new ArrayList<>();
                            binding.progressBar2.setVisibility(View.GONE);
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                                String id = document.getId();
                                String name = document.getString("name");
                                words = (List<String>) document.get("words");
                                binding.sampleHeader.setText(name);
                                if (words != null && !words.isEmpty()) {
                                    wordsByList.put(id, new ArrayList<>(words));
                                }
                            } else {
                                Log.d(TAG, "No such document");
                            }


                        }

                    }
                });
    }*/
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
