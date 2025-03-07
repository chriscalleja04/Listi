package com.example.listi.ui.lists;

import static android.content.ContentValues.TAG;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.listi.MyAdapter;
import com.example.listi.R;
import com.example.listi.RecyclerViewInterface;
import com.example.listi.UserViewModel;
import com.example.listi.WordList;
import com.example.listi.databinding.FragmentHomeBinding;
import com.example.listi.databinding.FragmentListsBinding;
import com.example.listi.databinding.FragmentStudentManagementBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ListsFragment extends Fragment implements RecyclerViewInterface {
    private UserViewModel userViewModel;
    private FirebaseFirestore db;
    private FragmentListsBinding binding;

    ArrayList<WordList> listArrayList;

    MyAdapter myAdapter;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        db = FirebaseFirestore.getInstance();


        binding = FragmentListsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        listArrayList = new ArrayList<WordList>();
        myAdapter = new MyAdapter(requireContext(), listArrayList, this);

        binding.recyclerView.setAdapter(myAdapter);

        binding.floatingActionButton.setVisibility(View.GONE);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        if (userViewModel.getUser().getValue() != null) {

            String userId = Objects.requireNonNull(userViewModel.getUser().getValue()).getUid();
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String role = document.getString("role");
                                    binding.floatingActionButton.setOnClickListener(v -> {
                                        Navigation.findNavController(v).navigate(R.id.action_listsFragment_to_newListFragment);
                                    });
                                    if (role != null) {
                                        if (role.equals("admin") || role.equals("educator")) {
                                            binding.floatingActionButton.setVisibility(View.VISIBLE);
                                        } else {
                                            binding.floatingActionButton.setVisibility(View.GONE);
                                        }


                                    }
                                } else {
                                    Log.d(TAG, "No such document");
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                            }
                        }
                    });

            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String email = document.getString("email");
                                    db.collectionGroup("students")
                                            .whereEqualTo("email", email)
                                            .limit(1)
                                            .get()
                                            .addOnCompleteListener(studentTask -> {
                                                if (studentTask.isSuccessful()) {
                                                    QuerySnapshot studentSnapshot = studentTask.getResult();
                                                    if (studentSnapshot != null && !studentSnapshot.isEmpty()) {
                                                        DocumentSnapshot doc = studentSnapshot.getDocuments().get(0);
                                                        String yearGroupId = doc.getString("yearGroupId");
                                                        String classId = doc.getString("classId");
                                                        String schoolId = doc.getString("schoolId");
                                                        db.collection("schools")
                                                                .document(schoolId)
                                                                .collection("yearGroups")
                                                                .document(yearGroupId)
                                                                .collection("classes")
                                                                .document(classId)
                                                                .collection("lists").orderBy("name", Query.Direction.ASCENDING)
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
                                                                                listArrayList.add(document.getDocument().toObject(WordList.class));
                                                                            }
                                                                        }

                                                                        // Notify the adapter of data changes
                                                                        myAdapter.notifyDataSetChanged();
                                                                    }


                                                                });
                                                    }
                                                } else {
                                                    Log.d(TAG, "Error getting documents: ", task.getException());

                                                }

                                            });

                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }

                        ;
                    });
        }


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemClick(int position) {
        if (!listArrayList.isEmpty()) {
            WordList wordlist = listArrayList.get(position);
            ArrayList<String> words = wordlist.getWords();

            if (words != null && !words.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("words", words);

                Navigation.findNavController(requireView()).navigate(R.id.action_listsFragment_to_expandListFragment, bundle);
            }
        }
    }
}
