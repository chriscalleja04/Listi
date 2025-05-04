package com.example.listi.ui.accessibility;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.listi.ColourManager;
import com.example.listi.FontManager;
import com.example.listi.MainActivity;
import com.example.listi.R;
import com.example.listi.UserViewModel;
import com.example.listi.VisualsManager;
import com.example.listi.databinding.FragmentAccessibilityBinding;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccessibilityFragment extends Fragment {

    private FirebaseFirestore db;
    private Spinner yearGroupSpinner;
    private ArrayAdapter<String> adapter;
    private FragmentAccessibilityBinding binding;
    private UserViewModel userViewModel;
    private FontManager fontManager;

    private ColourManager colourManager;
    private Typeface comicSansTypeface, openDyslexicTypeface, andikaTypeface;


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

        andikaTypeface = ResourcesCompat.getFont(requireContext(), R.font.andika);
        binding.andika.setTypeface(andikaTypeface);

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

        binding.andika.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                fontManager.setFontType(FontManager.FONT_ANDIKA);
                Toast.makeText(requireContext(), "Font changed to Andika", Toast.LENGTH_SHORT).show();
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
        binding.imageButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colourManager.setColourType(ColourManager.COLOUR_3);
                Toast.makeText(requireContext(), "Colour changed to 3", Toast.LENGTH_SHORT).show();
                restartApp();
            }
        });

        VisualsManager visualsManager = new VisualsManager(requireContext());
        binding.imageButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visualsManager.setVisualsType(VisualsManager.VISUAL_1);
                Toast.makeText(requireContext(), "Config set to Chest", Toast.LENGTH_SHORT).show();
                restartApp();
            }
        });
        binding.imageButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                visualsManager.setVisualsType(VisualsManager.VISUAL_2);
                Toast.makeText(requireContext(), "Config set to Laptop", Toast.LENGTH_SHORT).show();
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
