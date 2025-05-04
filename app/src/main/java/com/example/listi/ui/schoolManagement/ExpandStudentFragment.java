package com.example.listi.ui.schoolManagement;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.listi.MainActivity;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentExpandStudentBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExpandStudentFragment extends Fragment {
    private LineChart mpLineChart;

    private BarChart mpBarChart;

    private int[] colorArray;
    String[] legendName = {"Mal-ewwel tentattiv", "Mat-tieni tentattiv", "Mat-tielet tentattiv", "Mar-raba' tentattiv", "Mal-ħames tentattiv", "Erġa' pprova"};
    private FragmentExpandStudentBinding binding;

    private UserViewModel userViewModel;

    private String studentId,childProfileId, statisticsId, attemptId;

    private FirebaseFirestore db;

    private List<String> wordsList = new ArrayList<>();
    private List<Integer> attemptsList = new ArrayList<>();

    private Map<String,Integer> wordAttemptsMap = new HashMap<>();

    private LineDataSet lineDataSet;

    private int barEntry = 0;




    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        db = FirebaseFirestore.getInstance();
        binding = FragmentExpandStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Context context = requireContext();
        colorArray = new int[] {
                ContextCompat.getColor(context, R.color.color1),
                ContextCompat.getColor(context, R.color.color2),
                ContextCompat.getColor(context, R.color.color3),
                ContextCompat.getColor(context, R.color.color4),
                ContextCompat.getColor(context, R.color.color5),
                ContextCompat.getColor(context, R.color.color6)
        };
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        studentId = getArguments() != null ? getArguments().getString("studentId", studentId) : null;
        childProfileId = getArguments() != null ? getArguments().getString("childProfileId", childProfileId) : null;
        statisticsId = getArguments() != null ? getArguments().getString("statisticsId", statisticsId) : null;
        attemptId = getArguments() != null ? getArguments().getString("attemptId", attemptId) : null;
        String schoolId = userViewModel.getSchoolID().getValue();
        String yearGroupId = userViewModel.getYearGroupID().getValue();
        String classRoomId = userViewModel.getClassRoomID().getValue();
        userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
            if (role.equals("public")) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    db.collection("users")
                            .document(currentUser.getUid())
                            .collection("childProfiles")
                            .document(childProfileId)
                            .collection("statistics")
                            .document(statisticsId)
                            .collection("attempts")
                            .orderBy("completedAt", Query.Direction.ASCENDING)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        wordsList.clear();

                                        // Use a Set to prevent duplicate words
                                        Set<String> uniqueWords = new HashSet<>();

                                        for (QueryDocumentSnapshot document : task.getResult()) {

                                            List<Map<String, Object>> wordAttempts = (List<Map<String, Object>>) document.get("wordAttempts");

                                            if (wordAttempts != null) {
                                                for (Map<String, Object> attempt : wordAttempts) {
                                                    String word = (String) attempt.get("word");
                                                    int attempts = Integer.parseInt((String) attempt.get("attempts"));

                                                    uniqueWords.add(word);
                                                  /*  wordAttemptsMap.putIfAbsent(word, new ArrayList<>());
                                                    wordAttemptsMap.get(word).add(attempts);*/


                                                }
                                            }
                                        }


                                        wordsList = new ArrayList<>(uniqueWords);


                                        //updateChart();
                                    }
                                }
                            });

                }

            } else if (role.equals("admin") || role.equals("educator")) {
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
                        .document(statisticsId)
                        .collection("attempts")
                        .document(attemptId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    wordsList.clear();
                                    DocumentSnapshot documentSnapshot = task.getResult();

                                    List<Map<String, String>> wordAttempts = (List<Map<String, String>>) documentSnapshot.get("wordAttempts");;
                                    // Use a Set to prevent duplicate words

                                    if (wordAttempts != null) {
                                        for (Map<String, String> attempt : wordAttempts) {
                                            String word = attempt.get("word");
                                            int attempts = Integer.parseInt(Objects.requireNonNull(attempt.get("attempts")));

                                            wordAttemptsMap.putIfAbsent(word, attempts);
                                            wordsList.add(word);
                                            attemptsList.add(attempts);

                                        }
                                    }
                                }





                                updateChart();
                            }
                        });

            }
        });
        return root;
    }

    private void updateChart() {
        mpBarChart = binding.barChart;
        mpBarChart.getDescription().setEnabled(false);


        Legend legendBar  = mpBarChart.getLegend();
        legendBar.setEnabled(true);
        legendBar.setTextSize(10);
        legendBar.setForm(Legend.LegendForm.SQUARE);
        legendBar.setFormSize(20);
        legendBar.setXEntrySpace(20);
        legendBar.setFormToTextSpace(20);
        legendBar.setWordWrapEnabled(true);

// Ensure the legend has enough space (adjusting max size percentage for more space)
        legendBar.setMaxSizePercent(0.7f); // Reduces the space the legend takes up in the chart

        mpBarChart.setExtraBottomOffset(10);

        LegendEntry[] legendEntries = new LegendEntry[6];

        for(int i = 0; i<legendEntries.length; i++){
            LegendEntry entry = new LegendEntry();
            entry.formColor = colorArray[i];
            entry.label = String.valueOf(legendName[i]);
            legendEntries[i] = entry;
        }

        legendBar.setCustom(legendEntries);


        ArrayList<BarEntry> dataVals = new ArrayList<>();
        for (int i = 0; i < wordsList.size(); i++) {
            String word = wordsList.get(i);
            int attempts = wordAttemptsMap.get(word);
            dataVals.add(new BarEntry(i, attempts));

        }
        BarDataSet barDataSet = new BarDataSet(dataVals, "Tentattivi");

        List<Integer> barColours = new ArrayList<>();
        for(int j=0; j<attemptsList.size(); j++) {
            int index = attemptsList.get(j) - 1;
            barColours.add(colorArray[index]);
        }
        barDataSet.setColors(barColours);
        barDataSet.setDrawValues(false);

        BarData dataBar = new BarData(barDataSet);
        dataBar.setBarWidth(0.9f);


        mpBarChart.setScaleEnabled(false);
        mpBarChart.setNoDataText("L-ebda data");
        mpBarChart.setNoDataTextColor(Color.BLUE);
        mpBarChart.setDrawGridBackground(true);
        mpBarChart.setTouchEnabled(false);




//        Description description = new Description();
//        description.setText("Zoo");
//        description.setTextColor(Color.BLUE);
//        description.setTextSize(20);
//        mpLineChart.setDescription(description);


        XAxis xAxisBar = mpBarChart.getXAxis();
        xAxisBar.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBar.setTextSize(12);
        xAxisBar.setYOffset(15);
        xAxisBar.setDrawGridLines(false);
        xAxisBar.setGranularityEnabled(true);
        xAxisBar.setGranularity(1);
        xAxisBar.setLabelRotationAngle(270); // Set rotation angle to 90 degrees
        xAxisBar.setLabelCount(wordsList.size()); // Ensure all labels are shown
        xAxisBar.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 0 && value < wordsList.size()) {
                    return wordsList.get((int) value);
                } else {

                    return "";
                }
            }
        });
        YAxis yAxisBar = mpBarChart.getAxisLeft();
        yAxisBar.setTextSize(15);
        yAxisBar.setXOffset(15);
        yAxisBar.setGranularity(1);
        yAxisBar.setAxisMinimum(0.5f);
        yAxisBar.setAxisMaximum(6.05f);
        yAxisBar.setGranularityEnabled(true);
        yAxisBar.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if(value == 6f){
                    return "Erġa' pprova";
                }else if(value>=1f && value<=5f){
                    return String.valueOf((int) value);
                }
                return "";
            }
        });




        YAxis rightYAxisBar = mpBarChart.getAxisRight();
        rightYAxisBar.setEnabled(false);

        mpBarChart.setClipValuesToContent(false);


        mpBarChart.setData(dataBar);
        mpBarChart.invalidate();


    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset orientation to the app's default behavior when leaving the fragment
        if (getActivity() != null) {
            if (((MainActivity)getActivity()).compactScreen()) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            }
        }
        binding = null;
    }
}

