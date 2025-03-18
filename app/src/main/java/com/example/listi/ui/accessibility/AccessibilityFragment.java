package com.example.listi.ui.accessibility;

import static android.content.ContentValues.TAG;

import static androidx.core.app.ActivityCompat.recreate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.listi.AzureTTSHelper;
import com.example.listi.ColourManager;
import com.example.listi.FontManager;
import com.example.listi.MainActivity;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.databinding.FragmentAccessibilityBinding;
import com.example.listi.databinding.FragmentHomeBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AccessibilityFragment extends Fragment {

    private FirebaseFirestore db;
    private Spinner yearGroupSpinner;
    private ArrayAdapter<String> adapter;
    private FragmentAccessibilityBinding binding;
    private UserViewModel userViewModel;
    private FontManager fontManager;

    private ColourManager colourManager;
    private Typeface comicSansTypeface, openDyslexicTypeface;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        binding = FragmentAccessibilityBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        fontManager = new FontManager(requireContext());
        comicSansTypeface = ResourcesCompat.getFont(requireContext(), R.font.comic_sans);
        binding.comicSans.setTypeface(comicSansTypeface); ;

        openDyslexicTypeface = ResourcesCompat.getFont(requireContext(), R.font.open_dyslexic);
        binding.openDyslexic.setTypeface(openDyslexicTypeface);

        binding.comicSans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fontManager.setFontType(FontManager.FONT_COMIC_SANS);
                Toast.makeText(requireContext(), "Font changed to Comic Sans", Toast.LENGTH_SHORT).show();
                restartApp();
            }
        });

        binding.openDyslexic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fontManager.setFontType(FontManager.FONT_OPEN_DYSLEXIC);
                Toast.makeText(requireContext(), "Font changed to Open Dyslexic", Toast.LENGTH_SHORT).show();
                restartApp();
            }
        });

        colourManager = new ColourManager(requireContext());

        binding.imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colourManager.setColourType(ColourManager.COLOUR_1);
                Toast.makeText(requireContext(), "Colour changed to 1", Toast.LENGTH_SHORT).show();
                restartApp();

            }
        });
        binding.imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colourManager.setColourType(ColourManager.COLOUR_2);
                Toast.makeText(requireContext(), "Colour changed to 2", Toast.LENGTH_SHORT).show();
                restartApp();

            }
        });

        return root;
    }

    private void restartApp(){
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Activity activity = getActivity();
        if (activity != null){
            activity.finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
