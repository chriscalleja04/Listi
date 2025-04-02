package com.example.listi.ui.studentManagement;

import static android.content.ContentValues.TAG;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.listi.MyAdapter;
import com.example.listi.MyStudentListAdapter;
import com.example.listi.R;
import com.example.listi.RecyclerViewInterface;
import com.example.listi.Student;
import com.example.listi.StudentList;
import com.example.listi.UserViewModel;
import com.example.listi.WordList;
import com.example.listi.databinding.FragmentListsBinding;
import com.example.listi.databinding.FragmentStudentListsBinding;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StudentListsFragment extends Fragment implements RecyclerViewInterface {
    private UserViewModel userViewModel;
    private FirebaseFirestore db;

    private FragmentStudentListsBinding binding;

    ArrayList<StudentList> studentListArray;

    MyStudentListAdapter studentListAdapter;

    String studentId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        db = FirebaseFirestore.getInstance();
        binding = FragmentStudentListsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);


        binding.studentListsRecyclerView.setHasFixedSize(true);
        binding.studentListsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        studentListArray = new ArrayList<StudentList>();
        studentListAdapter = new MyStudentListAdapter(requireContext(), studentListArray, this);
        binding.studentListsRecyclerView.setAdapter(studentListAdapter);

        studentId = getArguments() != null ? getArguments().getString("id") : null;
        String schoolId = userViewModel.getSchoolID().getValue();
        String yearGroupId = userViewModel.getYearGroupID().getValue();
        String classRoomId = userViewModel.getClassRoomID().getValue();

        assert schoolId != null;
        assert yearGroupId != null;
        assert classRoomId != null;
        db.collection("schools")
                .document(schoolId)
                .collection("yearGroups")
                .document(yearGroupId)
                .collection("classes")
                .document(classRoomId)
                .collection("students")
                .document(studentId)
                .collection("statistics")
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
                                StudentList studentList = document.getDocument().toObject(StudentList.class);
                                studentList.setId(document.getDocument().getId());
                                studentListArray.add(studentList);

                            }
                        }

                        // Notify the adapter of data changes
                        studentListAdapter.notifyDataSetChanged();
                    }


                });
        return root;
        }


    @Override
    public void onItemClick(int position) {
        if (!studentListArray.isEmpty()) {
            StudentList studentList = studentListArray.get(position);
            String statisticsId = studentList.getId();

            if (statisticsId != null && !statisticsId.isEmpty()) {

                Bundle bundle = new Bundle();
                bundle.putString("studentId", studentId);
                bundle.putString("statisticsId", statisticsId);
                Navigation.findNavController(requireView()).navigate(R.id.action_studentListsFragment_to_expandStudentFragment, bundle);

            }
        }



    }
}