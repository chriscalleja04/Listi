package com.example.listi.ui.studentManagement;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.listi.MyStudentAdapter;
import com.example.listi.R;
import com.example.listi.RecyclerViewInterface;
import com.example.listi.Student;
import com.example.listi.UserViewModel;
import com.example.listi.YearClassRepository;
import com.example.listi.databinding.FragmentNewStudentBinding;
import com.example.listi.databinding.FragmentStaffManagementBinding;
import com.example.listi.databinding.FragmentStudentManagementBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
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

public class StudentManagementFragment extends Fragment implements RecyclerViewInterface{
    private FirebaseFirestore db;
    private Spinner yearGroupSpinner, classRoomSpinner;
    private ArrayAdapter<String> adapterYearGroup, adapterClassRoom;

    private FragmentStudentManagementBinding binding;
    private UserViewModel userViewModel;
    private YearClassRepository yearClassRepository;

    private RecyclerView recyclerView;
    private ArrayList<Student> studentArrayList;
    private MyStudentAdapter myStudentAdapter;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentStudentManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.recyclerView2.setHasFixedSize(true);
        binding.recyclerView2.setLayoutManager(new LinearLayoutManager(requireContext()));
        db = FirebaseFirestore.getInstance();
        studentArrayList = new ArrayList<Student>();
        myStudentAdapter = new MyStudentAdapter(requireContext(), studentArrayList, this);


        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        yearClassRepository = new YearClassRepository(userViewModel);
        binding.floatingActionButton2.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_studentManagementFragment_to_newStudentFragment);
        });

        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                userViewModel.getSchoolName().observe(getViewLifecycleOwner(), schoolName -> {
                    binding.schoolNameStudent2.setText(schoolName);
                });

                yearGroupSpinner = binding.yearGroupStudent2;
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


                classRoomSpinner = binding.classSpinnerStudent2;
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
                        classRoomSpinner.setSelection(0);
                        myStudentAdapter.notifyDataSetChanged();
                        studentArrayList.clear();
                        String selectedYearGroup = adapterView.getItemAtPosition(i).toString();
                        binding.recyclerView2.setAdapter(myStudentAdapter);
                        yearClassRepository.fetchYearGroupID(selectedYearGroup, getViewLifecycleOwner());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                classRoomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        studentArrayList.clear();
                        String selectedClass = classRoomSpinner.getSelectedItem().toString();
                        EventChangeListener(selectedClass);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });





            } else {
                binding.schoolNameStudent2.setText("Not Signed in");
                binding.yearGroupStudent2.setAdapter(null);
                binding.classSpinnerStudent2.setAdapter(null);
            }
        });

        return root;
    }

    private void EventChangeListener(String selectedClassRoomName){
        yearClassRepository.fetchClassRoomIDByName(selectedClassRoomName).addOnCompleteListener(task -> { // Fetch ClassRoomID here based on name
            if (task.isSuccessful()) {
                String classRoomID = task.getResult();
                String schoolID = userViewModel.getSchoolID().getValue();
                String yearGroupID = userViewModel.getYearGroupID().getValue();
                assert schoolID != null;
                assert yearGroupID != null;
                db.collection("schools")
                        .document(schoolID)
                        .collection("yearGroups")
                        .document(yearGroupID)
                        .collection("classes")
                        .document(classRoomID)
                        .collection("students").orderBy("name", Query.Direction.ASCENDING)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                if(error != null){
                                    Log.e("Firestore error", Objects.requireNonNull(error.getMessage()));
                                    return;
                                }
                                assert value != null;
                                for(DocumentChange document: value.getDocumentChanges()){
                                    if(document.getType() == DocumentChange.Type.ADDED){
                                        Student student = document.getDocument().toObject(Student.class);
                                        student.setID(document.getDocument().getId());
                                        studentArrayList.add(student);
                                    }
                                    myStudentAdapter.notifyDataSetChanged();
                                }
                            }
                        });

            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemClick(int position) {
        if(!studentArrayList.isEmpty()){
            Student student = studentArrayList.get(position);
            String id = student.getID();

            if(id != null && !id.isEmpty()){
                Bundle bundle = new Bundle();
                bundle.putString("id", id);

                Navigation.findNavController(requireView()).navigate(R.id.action_studentManagementFragment_to_studentListsFragment, bundle);
            }
        }
    }
}