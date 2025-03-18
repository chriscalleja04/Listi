package com.example.listi.ui.lists;

import static android.content.ContentValues.TAG;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

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

import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentListsBinding;
import com.example.listi.databinding.FragmentNewListBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewListFragment extends Fragment {

    private FragmentNewListBinding binding;
    private Typeface comicSansTypeface;
    private FirebaseFirestore db;
    private Spinner yearGroupSpinner, classRoomSpinner;
    private ArrayAdapter<String> adapterYearGroup, adapterClassRoom;

    private List<EditText> editTextList = new ArrayList<>();
    private ArrayList<String> wordsList = new ArrayList<>();
    private UserViewModel userViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();

        // Initialize binding
        binding = FragmentNewListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Load Comic Sans from res/font/
        comicSansTypeface = ResourcesCompat.getFont(requireContext(), R.font.comic_sans);
        editTextList.add(binding.wordInput);
        // Set click listener for Floating Action Button
        binding.addWord.setOnClickListener(v -> addNewWordField());
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                userViewModel.getSchoolName().observe(getViewLifecycleOwner(), schoolName -> {
                    binding.schoolName.setText(schoolName);
                });

                yearGroupSpinner = binding.yearGroup;
                adapterYearGroup = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>()
                );
                adapterYearGroup.setDropDownViewResource(android.R.layout.simple_spinner_item);
                yearGroupSpinner.setAdapter(adapterYearGroup);

                userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                    if (role.equals("admin")) {
                        userViewModel.getYearGroups().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> yearGroups) {
                                if (yearGroups != null) {
                                    adapterYearGroup.clear();
                                    adapterYearGroup.addAll(yearGroups);
                                    adapterYearGroup.notifyDataSetChanged();
                                }
                            }
                        });
                    } else if (role.equals("educator")) {
                        userViewModel.getYearGroupsEducator().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> yearGroups) {
                                if (yearGroups != null) {
                                    adapterYearGroup.clear();
                                    adapterYearGroup.addAll(yearGroups);
                                    adapterYearGroup.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                });


                classRoomSpinner = binding.classSpinner;
                adapterClassRoom = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        new ArrayList<>()
                );
                adapterClassRoom.setDropDownViewResource(android.R.layout.simple_spinner_item);
                classRoomSpinner.setAdapter(adapterClassRoom);


                userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                    if (role.equals("admin")) {
                        userViewModel.getClasses().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> classes) {
                                if (classes != null) {
                                    adapterClassRoom.clear();
                                    adapterClassRoom.addAll(classes);
                                    adapterClassRoom.notifyDataSetChanged();
                                }
                            }
                        });
                    } else if (role.equals("educator")) {
                        userViewModel.getClassesEducator().observe(getViewLifecycleOwner(), new Observer<List<String>>() {
                            @Override
                            public void onChanged(List<String> classes) {
                                if (classes != null) {
                                    adapterClassRoom.clear();
                                    adapterClassRoom.addAll(classes);
                                    adapterClassRoom.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                });

                yearGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        String selectedYearGroup = adapterView.getItemAtPosition(i).toString();
                        fetchYearGroupID(selectedYearGroup);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                classRoomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });


            } else {
                binding.schoolName.setText("Not Signed in");
                binding.yearGroup.setAdapter(null);
                binding.classSpinner.setAdapter(null);
            }
        });
        binding.addList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = binding.listName.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Please Enter a Name to Submit", Toast.LENGTH_SHORT).show();
                } else {
                    String selectedClass = classRoomSpinner.getSelectedItem().toString(); // Get selected class name

                    saveWords();
                    saveNewList(name, wordsList, selectedClass); // Pass selected class name to saveNewStudent
                }
            }
        });


        return root;
    }

    private void addNewWordField() {
        if (binding == null) return; // Safety check

        // Create a new EditText
        EditText newWordInput = new EditText(requireContext());
        newWordInput.setHint("Ikteb kelma");
        newWordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        // Apply Comic Sans font
        newWordInput.setTypeface(comicSansTypeface);

        // Set layout params
        newWordInput.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Add the EditText inside the container
        binding.wordInputContainer.addView(newWordInput);
        editTextList.add(newWordInput);
        Log.d(TAG, "EditText added! Total: " + editTextList.size()); // Debugging

    }

    private void saveWords(){
        wordsList.clear();
        Log.d(TAG, "Total EditTexts in List: " + editTextList.size()); // Debugging

        for(EditText editText:editTextList){
            String word = editText.getText().toString().trim();
            if(!word.isEmpty()){
                wordsList.add(word);
            }
        }
        Log.d(TAG, "Words Saved: " + wordsList.toString()); // Debugging

    }
    public void fetchYearGroupID(String selectedYearGroup) {
        String schoolID = userViewModel.getSchoolID().getValue();
        CollectionReference schoolsRef = db.collection("schools");
        assert schoolID != null;
        DocumentReference schoolIDRef = schoolsRef.document(schoolID);
        CollectionReference yearGroupsRef = schoolIDRef.collection("yearGroups");
        yearGroupsRef.whereEqualTo("name", selectedYearGroup)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                String yearGroupID = querySnapshot.getDocuments().get(0).getId();
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                assert currentUser != null;
                                String email = currentUser.getEmail();
                                userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                                    fetchClassRooms(yearGroupID, role, email);
                                });
                                userViewModel.setYearGroupID(yearGroupID);
                                Log.d("Firestore", "Year Group ID: " + yearGroupID);

                            } else {
                                Log.d(TAG, "Doc does not exist");

                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public Task<String> fetchClassRoomIDByName(String selectedClassRoom) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupID = userViewModel.getYearGroupID().getValue();
        assert schoolID != null;
        assert yearGroupID != null;
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupID)
                .collection("classes")
                .whereEqualTo("name", selectedClassRoom)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                String classRoomID = querySnapshot.getDocuments().get(0).getId();
                                taskCompletionSource.setResult(classRoomID);
                            } else {
                                Log.d(TAG, "Name does not exist");
                                taskCompletionSource.setException(new Exception("Classroom name does not exist")); // Or handle no class found differently
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            taskCompletionSource.setException(task.getException());
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }


    public void fetchClassRooms(String yearGroupId, String role, String email) {
        String schoolID = userViewModel.getSchoolID().getValue();
        assert schoolID != null;
        if (role.equals("admin")) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                List<String> classes = new ArrayList<>();
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String name = document.getString("name");
                                    if (name != null) {
                                        classes.add(name);
                                    }
                                }
                                userViewModel.setClasses(classes);
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        } else {
            if (role.equals("educator")) {
                fetchEducatorClasses(email).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> classIds = task.getResult();
                        if (classIds != null && !classIds.isEmpty()) {
                            db.collection("schools")
                                    .document(schoolID)
                                    .collection("yearGroups")
                                    .document(yearGroupId)
                                    .collection("classes")
                                    .whereIn(FieldPath.documentId(), classIds)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                List<String> classRoomNames = new ArrayList<>();
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    String name = document.getString("name");
                                                    if (name != null) {
                                                        classRoomNames.add(name);
                                                    }
                                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                                }
                                                userViewModel.setClassesEducator(classRoomNames);
                                            } else {
                                                Log.d(TAG, "Error getting documents: ", task.getException());
                                            }
                                        }
                                    });
                        }
                    }
                });
            }
        }
    }

    public Task<List<String>> fetchEducatorClasses(String email) {
        TaskCompletionSource<List<String>> taskCompletionSource = new TaskCompletionSource<>();
        List<String> result = new ArrayList<>();

        db.collectionGroup("educators")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(educatorTask -> {
                    if (educatorTask.isSuccessful()) {
                        QuerySnapshot querySnapshot = educatorTask.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot doc : educatorTask.getResult()) {
                                String classId = doc.getString("classRoomId");
                                if (classId != null) {
                                    result.add(classId);
                                }
                                Log.d(TAG, "Found educator in path: " + doc.getReference().getPath());
                            }
                            taskCompletionSource.setResult(result);
                        } else {
                            // No documents found
                            Log.d(TAG, "No educators found for email: " + email);
                            taskCompletionSource.setResult(result); // Return empty list
                        }
                    } else {
                        Log.e(TAG, "Error in educator query", educatorTask.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    // Modified saveNewStudent to accept selectedClassRoomName
    public void saveNewList(String name, List<String> wordsList, String selectedClassRoomName) {

        fetchClassRoomIDByName(selectedClassRoomName).addOnCompleteListener(task -> { // Fetch ClassRoomID here based on name
            if (task.isSuccessful()) {
                String classRoomID = task.getResult();
                String schoolID = userViewModel.getSchoolID().getValue();
                String yearGroupID = userViewModel.getYearGroupID().getValue();
                Map<String, Object> listDetails = new HashMap<>();
                listDetails.put("name", name);
                listDetails.put("schoolId", schoolID);
                listDetails.put("classId", classRoomID);
                listDetails.put("words", wordsList);
                assert schoolID != null;
                assert yearGroupID != null;
                db.collection("schools")
                        .document(schoolID)
                        .collection("yearGroups")
                        .document(yearGroupID)
                        .collection("classes")
                        .document(classRoomID)
                        .collection("lists")
                        .add(listDetails)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Toast.makeText(requireContext(), "Suċċess! Il-Lista inħalqet bla problemi", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "DocumentSnapshot successfully written!");
                                binding.listName.setText("");
                                for(EditText editText:editTextList){
                                    editText.setText("");

                                }
                                binding.wordInputContainer.removeViews(1, binding.wordInputContainer.getChildCount()-1);

                                binding.yearGroup.setSelection(0);
                                binding.classSpinner.setSelection(0);

                            };
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Error adding List. Could not fetch class ID.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error fetching ClassRoomID: ", e);
                        });


            } else {
                Toast.makeText(requireContext(), "Could not fetch Class ID. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}