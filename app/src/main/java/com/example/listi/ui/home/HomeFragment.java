package com.example.listi.ui.home;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.annotation.Nullable;
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
import com.google.android.material.button.MaterialButton;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {
    private FirebaseFirestore db;
    private Spinner yearGroupSpinner;
    private ArrayAdapter<String> adapter;
    private FragmentHomeBinding binding;
    private UserViewModel userViewModel;
    private ArrayList<WordList> sampleList = new ArrayList<>();

    private List<String> sampleWords;
    private Map<String, List<String>> wordsByList = new HashMap<>();



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        userViewModel.getIsNetworkAvailable().observe(getViewLifecycleOwner(), isAvailable -> {
            if (isAvailable) {
                binding.connectionStatusTextView.setVisibility(View.GONE);
            } else {
                binding.connectionStatusTextView.setVisibility(View.VISIBLE);
            }
        });

        db = FirebaseFirestore.getInstance();
        Context context = getActivity();


        List<String> wordsSample = new ArrayList<>();
        wordsSample.add("kien");
        wordsSample.add("biex");
        wordsSample.add("minn");
        wordsSample.add("hemm");
        wordsSample.add("fuq");
        wordsSample.add("ħafna");
        wordsSample.add("jien");
        wordsSample.add("test");
        wordsSample.add("imma");
        wordsSample.add("dan");

        wordsByList.put("sampleList", wordsSample);

        SharedPreferences sharedPreferences = context.getSharedPreferences("words", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(wordsByList);
        editor.putString("wordsByList", json);

        editor.apply();



        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.account.setText("Profil");
                binding.account.setIconResource(R.drawable.account_circle_24dp_000000_fill0_wght400_grad0_opsz24);
                binding.account.setOnClickListener(v -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_profileFragment);

                });

            } else {
                binding.account.setText("Idħol");
                binding.account.setIconResource(R.drawable.login_24dp_000000_fill0_wght400_grad0_opsz24);
                binding.account.setOnClickListener(v -> {
                    Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_authenticateFragment);

                });
            }
        });



        binding.sample.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sharedPreferences = context.getSharedPreferences("words", Context.MODE_PRIVATE);

                Bundle bundle = new Bundle();
                String json = sharedPreferences.getString("wordsByList", "");
                bundle.putString("wordsJson", json);

                Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_expandListFragment, bundle);

            }
        });

        binding.keyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.nav_help);
            }
        });





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
