package com.example.listi.ui.studentManagement;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentExpandStudentBinding;
import com.example.listi.databinding.FragmentStudentManagementBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ExpandStudentFragment extends Fragment {
    private LineChart mpLineChart;

    private BarChart mpBarChart;

    private int colorArray[] = {Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.LTGRAY, Color.DKGRAY};
    private int[] colourClassArray;

    private FragmentExpandStudentBinding binding;

    private UserViewModel userViewModel;

    private String studentId, statisticsId;

    private FirebaseFirestore db;

    private List<String> wordsList = new ArrayList<>();

    private Map<String,List<Integer>> wordAttemptsMap = new HashMap<>();

    private LineDataSet lineDataSet;

    private int entry = 1;




    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        binding = FragmentExpandStudentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        studentId = getArguments() != null ? getArguments().getString("studentId", studentId) : null;
        statisticsId = getArguments() != null ? getArguments().getString("statisticsId", statisticsId) : null;
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
                                        wordAttemptsMap.putIfAbsent(word, new ArrayList<>());
                                        wordAttemptsMap.get(word).add(attempts);


                                    }
                                }
                            }


                            wordsList = new ArrayList<>(uniqueWords);


                            updateChart();
                        }
                    }
                });





        return root;
    }

    private void updateChart() {
        mpLineChart = binding.lineChart;
        mpBarChart = binding.barChart;

        Legend legend  = mpLineChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(Color.RED);
        legend.setTextSize(15);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setFormSize(20);
        legend.setXEntrySpace(20);
        legend.setFormToTextSpace(10);

        Legend legendBar  = mpBarChart.getLegend();
        legendBar.setEnabled(true);
        legendBar.setTextSize(15);
        legendBar.setForm(Legend.LegendForm.SQUARE);
        legendBar.setFormSize(20);
        legendBar.setXEntrySpace(20);
        legendBar.setFormToTextSpace(10);
        mpBarChart.setExtraBottomOffset(20);



        colourClassArray = new int[wordsList.size()];
        for(int i = 0; i < wordsList.size(); i++){
            colourClassArray[i] = colorArray[i % colorArray.length];
        }


        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        ArrayList<IBarDataSet> dataSetsBar = new ArrayList<>();
        for(int i = 0; i < wordsList.size(); i++) {
            List<Integer> attempts = wordAttemptsMap.get(wordsList.get(i));
            if (attempts != null) {
                if((attempts.size()==1)){
                    mpBarChart.setVisibility(View.VISIBLE);
                    mpLineChart.setVisibility(View.INVISIBLE);
                    BarDataSet barDataSet = new BarDataSet(dataValuesBar(wordsList.get(i)), wordsList.get(i));
                    dataSetsBar.add(barDataSet);
                    barDataSet.setColor(colourClassArray[i]);
                    barDataSet.setDrawValues(false);


                } else {
                    mpLineChart.setVisibility(View.VISIBLE);
                    mpBarChart.setVisibility(View.INVISIBLE);
                    LineDataSet lineDataSet = new LineDataSet(dataValues(wordsList.get(i)), wordsList.get(i));
                    dataSets.add(lineDataSet);
                    lineDataSet.setColor(colourClassArray[i]);
                    lineDataSet.setDrawValues(false);
                    lineDataSet.setLineWidth(4);
                    lineDataSet.setDrawCircles(true);
                    lineDataSet.setDrawCircleHole(true);
                    lineDataSet.setCircleColor(Color.GRAY);


                    lineDataSet.setCircleRadius(8); //circle radius must be bigger than circle hole radius
                    lineDataSet.setCircleHoleRadius(5);
                    lineDataSet.setValueTextSize(10);
                    lineDataSet.setCircleHoleColor(Color.WHITE);
                }
            }

        }

                mpLineChart.setScaleEnabled(false);
                mpLineChart.setNoDataText("No Data");
                mpLineChart.setNoDataTextColor(Color.BLUE);
                mpLineChart.setDrawGridBackground(true);

                mpBarChart.setScaleEnabled(false);
                mpBarChart.setNoDataText("No Data");
                mpBarChart.setNoDataTextColor(Color.BLUE);
                mpBarChart.setDrawGridBackground(true);




                /*mpLineChart.setDrawBorders(true);
                mpLineChart.setBorderColor(Color.GRAY);
                mpLineChart.setBorderWidth(1);*/

//        Description description = new Description();
//        description.setText("Zoo");
//        description.setTextColor(Color.BLUE);
//        description.setTextSize(20);
//        mpLineChart.setDescription(description);

                XAxis xAxis = mpLineChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setTextSize(15);
                xAxis.setYOffset(15);
                xAxis.setAxisMinimum(1);
                xAxis.setGranularity(1);
                xAxis.setGranularityEnabled(true);


                YAxis yAxis = mpLineChart.getAxisLeft();
                yAxis.setAxisMinimum(0.5f);
                yAxis.setTextSize(15);
                yAxis.setXOffset(15);
                yAxis.setGranularity(1);
                yAxis.setAxisMaximum(6.05f);
                yAxis.setGranularityEnabled(true);
                yAxis.setSpaceTop(10);
                yAxis.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        if(value == 6f){
                            return "incorrect";
                        }else if(value>=1f && value<=5f){
                            return String.valueOf((int) value);
                        }
                        return "";
                    }
                });

                YAxis rightYAxis = mpLineChart.getAxisRight();
                rightYAxis.setEnabled(false);

                mpLineChart.setClipValuesToContent(false);




        LineData data = new LineData(dataSets);
        mpLineChart.setData(data);
        mpLineChart.invalidate();


        XAxis xAxisBar = mpBarChart.getXAxis();
        xAxisBar.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBar.setTextSize(15);
        xAxisBar.setYOffset(15);
        xAxisBar.setDrawGridLines(false);
        xAxisBar.setGranularityEnabled(true);
        xAxisBar.setGranularity(1);
        xAxisBar.setLabelCount(1, true);
        xAxisBar.setCenterAxisLabels(true);
        xAxisBar.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "1";
            }
        });

        YAxis yAxisBar = mpBarChart.getAxisLeft();
        yAxisBar.setTextSize(15);
        yAxisBar.setXOffset(15);
        yAxisBar.setGranularity(1);
        yAxisBar.setAxisMaximum(6.05f);
        yAxisBar.setGranularityEnabled(true);
        yAxisBar.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if(value == 6f){
                    return "incorrect";
                }else if(value>=1f && value<=5f){
                    return String.valueOf((int) value);
                }
                return "";
            }
        });
/*

       */

        YAxis rightYAxisBar = mpBarChart.getAxisRight();
        rightYAxisBar.setEnabled(false);

        mpBarChart.setClipValuesToContent(false);


        BarData dataBar = new BarData(dataSetsBar);
        mpBarChart.setData(dataBar);
        mpBarChart.invalidate();


    }





    private ArrayList<Entry> dataValues(String word) {
        entry = 1;
        ArrayList<Entry> dataVals = new ArrayList<Entry>();
        List<Integer> attempts = wordAttemptsMap.get(word);
        if (attempts != null) {
            for (int i = 0; i < attempts.size(); i++) {
                dataVals.add(new Entry(entry, attempts.get(i)));
                entry++;
            }



        }
        return dataVals;

    }

    private ArrayList<BarEntry> dataValuesBar(String word) {
        ArrayList<BarEntry> dataVals = new ArrayList<BarEntry>();
        List<Integer> attempts = wordAttemptsMap.get(word);
        if (attempts != null) {
            for (int i = 0; i < attempts.size(); i++) {
                dataVals.add(new BarEntry(entry, attempts.get(i)));
                entry++;
            }



        }
        return dataVals;

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}