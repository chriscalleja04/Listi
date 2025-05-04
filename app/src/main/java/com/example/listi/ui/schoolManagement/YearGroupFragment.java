package com.example.listi.ui.schoolManagement;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.listi.MyYearGroupsAdapter;
import com.example.listi.R;
import com.example.listi.RecyclerViewInterface;
import com.example.listi.YearGroup;
import com.example.listi.UserViewModel;
import com.example.listi.YearClassRepository;
import com.example.listi.databinding.FragmentStudentManagementBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class YearGroupFragment extends Fragment implements RecyclerViewInterface, MyYearGroupsAdapter.YearGroupActionListener {
    private FirebaseFirestore db;
    private FragmentStudentManagementBinding binding;
    private UserViewModel userViewModel;
    private YearClassRepository yearClassRepository;
    private ArrayList<YearGroup> yearGroupArrayList;
    private MyYearGroupsAdapter myYearGroupsAdapter;
    private String yearGroupIdToDelete;
    private FloatingActionButton edit, delete;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentStudentManagementBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.recyclerView2.setHasFixedSize(true);
        binding.recyclerView2.setLayoutManager(new LinearLayoutManager(requireContext()));
        db = FirebaseFirestore.getInstance();

        yearGroupArrayList = new ArrayList<>();


        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        yearClassRepository = new YearClassRepository(userViewModel);

        binding.addYearGroup.setOnClickListener(v ->{
            Navigation.findNavController(v).navigate(R.id.action_yearGroupFragment_to_newYearGroupFragment);
        });

        userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
            // Set adapter with role
            myYearGroupsAdapter = new MyYearGroupsAdapter(requireContext(), yearGroupArrayList, this, this, role);
            binding.recyclerView2.setAdapter(myYearGroupsAdapter);

            if (!role.equals("admin")) {
                binding.addYearGroup.setVisibility(View.GONE);
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userViewModel.getSchoolID().observe(getViewLifecycleOwner(), schoolID -> {
                    yearClassRepository.fetchYearGroups(schoolID, role, currentUser.getEmail());

                    if (role.equals("educator")) {
                        userViewModel.getYearGroupsEducator().observe(getViewLifecycleOwner(), yearGroups -> {
                            yearGroupArrayList.clear();
                            yearGroupArrayList.addAll(yearGroups);
                            myYearGroupsAdapter.notifyDataSetChanged();
                        });
                    } else if (role.equals("admin")) {
                        userViewModel.getYearGroups().observe(getViewLifecycleOwner(), yearGroups -> {
                            yearGroupArrayList.clear();
                            yearGroupArrayList.addAll(yearGroups);
                            myYearGroupsAdapter.notifyDataSetChanged();
                        });
                    }
                });
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
      if(!yearGroupArrayList.isEmpty()){
          YearGroup yearGroup = yearGroupArrayList.get(position);
          String id = yearGroup.getID();

            if(id != null && !id.isEmpty()){
                Bundle bundle = new Bundle();
                bundle.putString("yearGroupId", id);

                Navigation.findNavController(requireView()).navigate(R.id.action_studentManagementFragment_to_classRoomFragment, bundle);
            }
        }
    }


    @Override
    public void onDeleteYearGroup(String yearGroupId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this year group?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (yearGroupId != null) {
                        yearClassRepository.deleteYearGroup(yearGroupId);

                        for (int i = 0; i < yearGroupArrayList.size(); i++) {
                            if (yearGroupArrayList.get(i).getID().equals(yearGroupId)) {
                                yearGroupArrayList.remove(i);
                                myYearGroupsAdapter.notifyItemRemoved(i);
                                break;
                            }
                        }

                        Snackbar.make(binding.getRoot(), "Year group deleted", Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", (dialog, which) -> {
                })
                .show();
    }
}