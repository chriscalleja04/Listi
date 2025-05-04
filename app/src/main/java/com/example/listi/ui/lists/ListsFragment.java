package com.example.listi.ui.lists;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
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
import com.example.listi.MyListsManagementAdapter;
import com.example.listi.Profile;
import com.example.listi.R;
import com.example.listi.RecyclerViewInterface;
import com.example.listi.UserViewModel;
import com.example.listi.WordList;
import com.example.listi.YearClassRepository;
import com.example.listi.databinding.FragmentHomeBinding;
import com.example.listi.databinding.FragmentListsBinding;
import com.example.listi.databinding.FragmentStudentManagementBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.snackbar.Snackbar;
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
import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class ListsFragment extends Fragment implements MyListsManagementAdapter.ListsActionListener{
    private UserViewModel userViewModel;
    private FirebaseFirestore db;
    private FragmentListsBinding binding;

    private final double MAX_WORDS = 10;

    ArrayList<WordList> listArrayList;

    MyAdapter myAdapter;

    MyListsManagementAdapter myParentAdapter;


    private YearClassRepository yearClassRepository;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        db = FirebaseFirestore.getInstance();


        binding = FragmentListsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        listArrayList = new ArrayList<WordList>();
        myAdapter = new MyAdapter(requireContext(), listArrayList);
        myParentAdapter = new MyListsManagementAdapter(requireContext(), listArrayList, this);

        binding.recyclerView.setAdapter(myAdapter);


        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        yearClassRepository = new YearClassRepository(userViewModel);
        binding.floatingActionButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_listsFragment_to_newListPublicFragment);
        });

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
                                    if (role != null) {
                                        //binding.floatingActionButton.setVisibility(View.GONE);
                                        if (role.equals("public")) {
                                            userViewModel.getChildID().observe(getViewLifecycleOwner(), id -> {
                                                if (id != null) {
                                                    binding.floatingActionButton.setVisibility(View.GONE);
                                                } else {
                                                    binding.floatingActionButton.setVisibility(View.VISIBLE);
                                                }
                                            });
                                        } else if (role.equals("student")) {
                                            binding.floatingActionButton.setVisibility(View.GONE);
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


            userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (role.equals("public")) {
                    userViewModel.getChildID().observe(getViewLifecycleOwner(), id -> {
                        if (id != null){
                            // Fetch child lists
                            binding.floatingActionButton.setVisibility(View.GONE);
                            binding.recyclerView.setAdapter(myAdapter); // Ensure myAdapter is set
                            listArrayList.clear(); // Clear previous data
                            assert currentUser != null;
                            db.collection("users")
                                    .document(currentUser.getUid())
                                    .collection("lists")
                                    .orderBy("name", Query.Direction.ASCENDING)
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                            // ... your existing snapshot listener for child lists
                                            if (error != null) {
                                                Log.e("Firestore error", error.getMessage());
                                                return;
                                            }
                                            if (value == null || value.isEmpty()) {
                                                Log.d(TAG, "No documents found for child.");
                                                // Consider showing a message to the user
                                                return;
                                            }
                                            listArrayList.clear(); // Clear before adding new data
                                            for (DocumentChange document : value.getDocumentChanges()) {
                                                if (document.getType() == DocumentChange.Type.ADDED) {
                                                    WordList wordList = document.getDocument().toObject(WordList.class);
                                                    wordList.setId(document.getDocument().getId());
                                                    listArrayList.add(wordList);
                                                }
                                                // Handle MODIFIED and REMOVED as well if needed
                                            }
                                            myAdapter.notifyDataSetChanged();
                                        }
                                    });
                        } /*else {
                            // Public user, no child selected
                            binding.play.setVisibility(View.GONE);
                            binding.floatingActionButton.setVisibility(View.VISIBLE);
                            binding.recyclerView.setAdapter(myParentAdapter); // Set myParentAdapter
                            listArrayList.clear(); // Clear previous data
                            yearClassRepository.fetchLists(); // Fetch parent lists
                            userViewModel.getLists().observe(getViewLifecycleOwner(), lists -> {
                                listArrayList.clear();
                                listArrayList.addAll(lists);
                                myParentAdapter.notifyDataSetChanged();
                            });
                        }*/
                    });

                    if (userViewModel.getChildID().getValue() == null) {
                        binding.play.setVisibility(View.GONE);
                        binding.floatingActionButton.setVisibility(View.VISIBLE);
                        binding.recyclerView.setAdapter(myParentAdapter); // Set myParentAdapter
                        listArrayList.clear(); // Clear previous data
                        yearClassRepository.fetchLists(); // Fetch parent lists
                        userViewModel.getLists().observe(getViewLifecycleOwner(), lists -> {
                            listArrayList.clear();
                            listArrayList.addAll(lists);
                            myParentAdapter.notifyDataSetChanged();
                        });
                    }

                } else if (role.equals("student")) {
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
                                                                String classId = doc.getString("classRoomId");
                                                                String schoolId = doc.getString("schoolId");
                                                                db.collection("schools")
                                                                        .document(schoolId)
                                                                        .collection("yearGroups")
                                                                        .document(yearGroupId)
                                                                        .collection("classes")
                                                                        .document(classId)
                                                                        .collection("lists")
                                                                        .orderBy("name", Query.Direction.ASCENDING)
                                                                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                                            @Override
                                                                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                                                                if (error != null) {
                                                                                    // Handle Firestore errors
                                                                                    Log.e("Firestore error", error.getMessage());
                                                                                    return;
                                                                                }

                                                                                if (value == null || value.isEmpty()) {
                                                                                    Log.d(TAG, "No documents found in the query.");
                                                                                    return;
                                                                                }

                                                                                for (DocumentChange document : value.getDocumentChanges()) {
                                                                                    if (document.getType() == DocumentChange.Type.ADDED) {
                                                                                        WordList wordList = document.getDocument().toObject(WordList.class);
                                                                                        wordList.setId(document.getDocument().getId());
                                                                                        listArrayList.add(wordList);

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
            });

        }

        binding.play.setOnClickListener(v -> {
            List<WordList> checkedLists = myAdapter.getCheckedItems();

            if (checkedLists.isEmpty()) {
                Toast.makeText(requireContext(), "Agħżel mill-inqas lista waħda biex tilgħab", Toast.LENGTH_SHORT).show();
            } else {
                Map<String, List<String>> wordsByList = new HashMap<>();
                Map<String, List<String>> wordsByListSample = new HashMap<>();
                Map<String, List<String>> incorrectWords = new HashMap<>();

                double calc = MAX_WORDS / checkedLists.size();
                int avg = (int) Math.floor(calc);

                for (int i = 0; i < checkedLists.size(); i++) {
                    WordList wordlist = checkedLists.get(i);
                    List<String> words = wordlist.getWords();
                    String id = wordlist.getId();
                    if (words != null && !words.isEmpty()) {
                        wordsByList.put(id, new ArrayList<>(words));
                    }
                }


                String currentId = null;
                Set<String> usedIds = new HashSet<>();
                for (Map.Entry<String, List<String>> entry : wordsByList.entrySet()) {
                    String shortestId = getShortestId(usedIds, wordsByList);

                    int[] indexes = new int[avg];
                    List<String> sWords = wordsByList.get(shortestId);
                    List<String> sWordsSample = new ArrayList<>();
                    if (sWords != null) {
                        if (sWords.size() <= avg) {
                            wordsByListSample.put(shortestId, new ArrayList<>(sWords));
                        } else {
                            List<String> sWordsCopy = new ArrayList<>(sWords);
                            Random rand = new Random();
                            Set<Integer> usedIndexes = new HashSet<>();
                            for (int j = 0; j < avg; j++) {
                                int index = rand.nextInt(sWordsCopy.size());
                                if (!usedIndexes.contains(index)) {
                                    usedIndexes.add(index);
                                    indexes[j] = index;
                                }
                            }
                            for (int k = 0; k < indexes.length; k++) {
                                sWordsSample.add(sWords.get(indexes[k]));
                            }
                            wordsByListSample.put(shortestId, new ArrayList<>(sWordsSample));


                        }
                        currentId = shortestId;
                        usedIds.add(currentId);
                    }


                }

                if (wordsByList.isEmpty()) {
                    Toast.makeText(requireContext(), "Ma nstabux kliem fil-listi magħżula", Toast.LENGTH_SHORT).show();
                } else {

                            Log.d(TAG, wordsByListSample.toString());
                            Bundle bundle = new Bundle();
                            Gson gson = new Gson();
                            String json = gson.toJson(wordsByListSample);

                            bundle.putString("wordsJson", json);

                            Navigation.findNavController(requireView()).navigate(R.id.action_listsFragment_to_expandListFragment, bundle);
                        }
                }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String getShortestId(Set<String> usedIds, Map<String, List<String>> wordsByList) {
        String shortestId = null;
        int minSize = Integer.MAX_VALUE;
        for (Map.Entry<String, List<String>> entry : wordsByList.entrySet()) {
            String id = entry.getKey();
            if (usedIds.contains(id)) {
                continue;
            }
            List<String> vals = entry.getValue();
            int size = vals.size();
            if (size < minSize) {
                minSize = size;
                shortestId = entry.getKey();
            }

        }

        return shortestId;
    }

    @Override
    public void onDeleteList(String listId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this List?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (listId != null) {
                        yearClassRepository.deleteList(listId);

                        for (int i = 0; i < listArrayList.size(); i++) {
                            if (listArrayList.get(i).getId().equals(listId)) {
                                listArrayList.remove(i);
                                myParentAdapter.notifyItemRemoved(i);
                                break;
                            }
                        }

                        Snackbar.make(binding.getRoot(), "Lista Tneħħiet", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                })
                .show();
    }

  /*  @Override
    public void onItemClick(int position) {
        if (!listArrayList.isEmpty()) {
            WordList wordlist = listArrayList.get(position);
            ArrayList<String> words = wordlist.getWords();
            String listId = wordlist.getId();
            String listName = wordlist.getName();

            if (words != null && !words.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("words", words);
                bundle.putString("listId", listId);
                bundle.putString("listName", listName);

                Navigation.findNavController(requireView()).navigate(R.id.action_listsFragment_to_expandListFragment, bundle);
            }
        }
    }*/


    }

