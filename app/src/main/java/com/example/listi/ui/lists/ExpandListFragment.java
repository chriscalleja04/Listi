package com.example.listi.ui.lists;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;


import com.example.listi.FontManager;
import com.example.listi.R;
import com.example.listi.StudentList;
import com.example.listi.UserViewModel;
import com.example.listi.VisualsManager;
import com.example.listi.databinding.FragmentExpandListBinding;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExpandListFragment extends Fragment {
    private FragmentExpandListBinding binding;
    private MediaPlayer mediaPlayer;
    private char[] letters;
    private List<String> lettersList = new ArrayList<>();

    private Map<String, List<String>> wordsJson;

    private List<String> words = new ArrayList<>();

    private Map<String, String> listIdWordMap = new HashMap<>();

    private String currentListId;

    private List<EditText> editTextList = new ArrayList<>();

    private List<ImageView> tickList = new ArrayList<>();

    private List<TextView> allWords = new ArrayList<>();
    private List<TextView> allAttempts = new ArrayList<>();


    private int currentWordIndex = 0;
    private final int MAX_ATTEMPTS = 5;
    private int incorrectCounter = 1;

    private enum Stage {PRE, LOOK, SAY, COVER, WRITE, CHECK, DONE}

    ;

    private Stage currentStage = Stage.LOOK;
    private Stage highestStageReached = Stage.LOOK;

    private static final String SUBSCRIPTION_KEY = "NC0lur3fQY1ba9oEOGMxUWDZf7aD6jw0KkI5b5nXdSuWAzoh1BYBJQQJ99BBACfhMk5XJ3w3AAAAACOG5tI5";
    private static final String REGION = "swedencentral";

    private File cachedAudioFile;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private List<Map<String, Object>> wordAttempts = new ArrayList<>();

    private ArrayList<StudentList> studentListArray;

    private StudentList studentList;

    private UserViewModel userViewModel;

    private VisualsManager visualsManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentExpandListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        visualsManager = new VisualsManager(requireContext());
        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);
        if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
            binding.chestClosed.setVisibility(View.VISIBLE);
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

        } else {
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.VISIBLE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);


        }

        binding.look.setOnClickListener(v -> {
            if (highestStageReached.equals(Stage.WRITE)) {
                Toast.makeText(getContext(), "Ipprova ikteb il-kelma qabel terġa taraha!", Toast.LENGTH_SHORT).show();
            } else {
                lookStage();
            }
        });
        binding.say.setOnClickListener(v -> {
            if (highestStageReached.equals(Stage.WRITE)) {
                Toast.makeText(getContext(), "Ipprova ikteb il-kelma qabel terġa tgħidha!", Toast.LENGTH_SHORT).show();
            } else {
                sayStage();
            }
        });
        binding.cover.setOnClickListener(v -> {
            if (highestStageReached.equals(Stage.WRITE)) {
                Toast.makeText(getContext(), "Ipprova ikteb il-kelma qabel tmur lura", Toast.LENGTH_SHORT).show();
            } else {
                coverStage();
            }
        });
        binding.write.setOnClickListener(v -> {
            writeStage();
        });
        binding.check.setOnClickListener(v -> {
            if (highestStageReached.equals(Stage.WRITE)) {
                Toast.makeText(getContext(), "Agħfas il-buttuna 'Kompli' biex tiċċikkeja l-kelma", Toast.LENGTH_SHORT).show();
            } else {
                checkStage();
            }
        });

        String json = getArguments() != null ? getArguments().getString("wordsJson") : "{}";
        Type type = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        wordsJson = new Gson().fromJson(json, type);

        for (Map.Entry<String, List<String>> entry : wordsJson.entrySet()) {
            String listId = entry.getKey();
            List<String> wordsList = entry.getValue();
            for (String word : wordsList) {
                listIdWordMap.put(word, listId);
                words.add(word);
            }
        }

        binding.progressText.setText("0%");
        if (words.isEmpty()) {
            return root;
        }

        setupButtonClickListener();
        loadCurrentWord();

        return root;
    }

    private void setupButtonClickListener() {
        binding.button.setOnClickListener(v -> {
            switch (currentStage) {
                case PRE:
                    preStage();
                    break;
                case LOOK:
                    lookStage();
                    break;

                case SAY:
                    sayStage();
                    break;

                case COVER:
                    coverStage();
                    break;

                case WRITE:
                    writeStage();
                    break;

                case CHECK:
                    checkStage();
                    break;

                case DONE:
                    doneStage();
                    break;

            }
        });
    }

    private void preStage() {
        binding.letterContainer.removeAllViews();

        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);

        if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.VISIBLE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

        } else {
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.VISIBLE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);
        }


        for (ImageView tick : tickList) {
            tick.setVisibility(View.GONE);
        }
        currentStage = Stage.LOOK;
    }

    private void lookStage() {
        binding.letterContainer.removeAllViews();

        String currentWord = words.get(currentWordIndex);
        binding.textView.setText(currentWord);
        if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.VISIBLE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

        } else {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.VISIBLE);
        }
        binding.textView.setVisibility(View.VISIBLE);
        binding.speaker.setVisibility(View.GONE);

        if (highestStageReached.ordinal() < Stage.SAY.ordinal()) {
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, outValue, true);
            int colorPrimary = outValue.data; // This is the resolved color value
            binding.look.setTextColor(colorPrimary);
            Drawable drawable = binding.imageView.getDrawable();
            drawable.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_IN);

            highestStageReached = Stage.LOOK;
        }
        currentStage = Stage.SAY;
        updateButtonStates();
    }

    public void sayStage() {
        if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.VISIBLE);
            binding.chestClosed.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

        } else {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.VISIBLE);
        }

        String currentWord = words.get(currentWordIndex);
        binding.textView.setVisibility(View.VISIBLE);
        binding.speaker.setVisibility(View.VISIBLE);
        binding.speaker.setOnClickListener(v -> {
                synthesizeText(currentWord);
        });

        if (highestStageReached.ordinal() < Stage.SAY.ordinal()) {
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, outValue, true);
            int colorPrimary = outValue.data; // This is the resolved color value
            binding.say.setTextColor(colorPrimary);

            Drawable drawable = binding.imageView2.getDrawable();
            drawable.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_IN);
            highestStageReached = Stage.SAY;
        }

        currentStage = Stage.COVER;
        updateButtonStates();


    }

    public void coverStage() {
        binding.letterContainer.removeAllViews();

        if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.VISIBLE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

        } else {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.VISIBLE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);
        }

        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);


        if (highestStageReached.ordinal() < Stage.COVER.ordinal()) {
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, outValue, true);
            int colorPrimary = outValue.data; // This is the resolved color value
            binding.cover.setTextColor(colorPrimary);

            Drawable drawable = binding.imageView3.getDrawable();
            drawable.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_IN);
            highestStageReached = Stage.COVER;
        }

        currentStage = Stage.WRITE;
    }

    public void writeStage() {
        LinearLayout letterContainer = binding.letterContainer;
        letterContainer.removeAllViews();
        editTextList.clear();
        tickList.clear();

        String word = words.get(currentWordIndex);

        LinearLayout editTextRow = new LinearLayout(getContext());
        editTextRow.setOrientation(LinearLayout.HORIZONTAL);
        editTextRow.setGravity(Gravity.CENTER);


        for (int i = 0; i < word.length(); i++) {
            LinearLayout letterTickPair = new LinearLayout(getContext());
            letterTickPair.setOrientation(LinearLayout.VERTICAL);
            letterTickPair.setGravity(Gravity.CENTER);
            ImageView tick = new androidx.appcompat.widget.AppCompatImageView(getContext());
            LinearLayout.LayoutParams tickParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tick.setLayoutParams(tickParams);
            tick.setImageResource(R.drawable.check_24dp_2dc937_fill0_wght400_grad0_opsz24);
            tick.setVisibility(View.INVISIBLE);
            tickList.add(tick);

            // Create EditText for each letter
            EditText letterInput = new androidx.appcompat.widget.AppCompatEditText(getContext());
            letterInput.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            letterInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            letterInput.setAutofillHints(String.valueOf(View.AUTOFILL_TYPE_NONE));
            letterInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)}); // Only 1 char
            letterInput.setEms(1);
            letterInput.setTextSize(18);
            //letterInput.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams letterParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            letterInput.setLayoutParams(letterParams);
            // In your EditText setup, modify the OnFocusChangeListener:
            letterInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        // Allow focusing on the field but control cursor position
                        letterInput.setSelection(letterInput.getText().length());
                    }
                }
            });
            letterInput.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        int position = editTextList.indexOf(letterInput);

                        if (letterInput.getText().length() > 0) {
                            // If current field is not empty, remove the character
                            letterInput.setText("");
                            return true; // Consume event
                        } else if (position > 0) {
                            // If current field is empty, move to previous field
                            EditText previousEditText = editTextList.get(position - 1);
                            previousEditText.requestFocus();
                            previousEditText.setText(""); // Clear previous field
                            previousEditText.setSelection(0); // Position cursor correctly
                            return true; // Consume event
                        }
                    }
                    return false; // Allow default behavior
                }
            });


            letterInput.setCursorVisible(false);

            editTextList.add(letterInput);
            //final int index = i; //for text watcher
            letterInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Not needed for forward navigation
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Not needed for forward navigation
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // If a character was typed, move to next field
                    if (s.length() == 1) {
                        int position = editTextList.indexOf(letterInput);
                        if (position < editTextList.size() - 1) {
                            EditText nextEditText = editTextList.get(position + 1);
                            nextEditText.requestFocus();
                        }
                    }
                }
            });
            letterTickPair.addView(tick);
            letterTickPair.addView(letterInput);
            // Add EditText and TextView to their respective rows
            editTextRow.addView(letterTickPair);
        }

        // Add both rows to the main container AFTER the loop
        letterContainer.addView(editTextRow);

        if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.VISIBLE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

        } else {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopOpen.setVisibility(View.VISIBLE);
            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
        }


        binding.speaker.setVisibility(View.GONE);

        binding.textView.setVisibility(View.GONE);
        if (highestStageReached.ordinal() < Stage.WRITE.ordinal()) {
            TypedValue outValue = new TypedValue();
            getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, outValue, true);
            int colorPrimary = outValue.data; // This is the resolved color value
            binding.write.setTextColor(colorPrimary);

            Drawable drawable = binding.imageView4.getDrawable();
            drawable.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_IN);


            highestStageReached = Stage.WRITE;
        }

        currentStage = Stage.CHECK;
        updateButtonStates();

    }

    private void checkStage() {
        String currentWord = words.get(currentWordIndex);
        boolean isCorrect = validateInput(currentWord);
       // binding.tickContainer.setVisibility(View.VISIBLE);

        if (isCorrect) {
            if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
                binding.chestClosed.setVisibility(View.GONE);
                binding.chestCheck.setVisibility(View.VISIBLE);
                binding.chestClosed.setVisibility(View.GONE);

                binding.laptopClosed.setVisibility(View.GONE);
                binding.laptopCheck.setVisibility(View.GONE);
                binding.laptopOpen.setVisibility(View.GONE);

            } else {
                binding.chestCheck.setVisibility(View.GONE);
                binding.chestClosed.setVisibility(View.GONE);
                binding.chestOpen.setVisibility(View.GONE);

                binding.laptopClosed.setVisibility(View.GONE);
                binding.laptopCheck.setVisibility(View.VISIBLE);
                binding.laptopOpen.setVisibility(View.GONE);
            }


            binding.speaker.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);
            binding.textView.setVisibility(View.GONE);
            mediaPlayer = MediaPlayer.create(getContext(), R.raw.success);
            mediaPlayer.start();
            if (highestStageReached.ordinal() < Stage.CHECK.ordinal()) {
                // Resolve ?attr/colorPrimary from the current theme
                TypedValue outValue = new TypedValue();
                getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, outValue, true);
                int colorPrimary = outValue.data; // This is the resolved color value
                binding.check.setTextColor(colorPrimary);

                Drawable drawable = binding.imageView5.getDrawable();
                drawable.setColorFilter(colorPrimary, PorterDuff.Mode.SRC_IN);

                binding.look.setEnabled(false);
                binding.say.setEnabled(false);
                binding.cover.setEnabled(false);
                binding.write.setEnabled(false);
                binding.check.setEnabled(false);
                highestStageReached = Stage.CHECK;
            }

            currentStage = Stage.DONE;
            Map<String, Object> wordAttempt = new HashMap<>();
            wordAttempt.put("word", currentWord);
            String incorrectCounterString = String.valueOf(incorrectCounter);
            wordAttempt.put("attempts", incorrectCounterString);
            wordAttempt.put("listId", currentListId);
            wordAttempts.add(wordAttempt);

            int nextWordIndex = currentWordIndex + 1;
            int percentCalc = Math.round(((float) nextWordIndex / words.size()) * 100);
            String percent = percentCalc + "%";
            binding.progressText.setText(percent);

            // Update progress visual
            if (percentCalc >= 25 && percentCalc < 50) {
                binding.glass.setImageResource(R.drawable.glass_25);
            } else if (percentCalc >= 50 && percentCalc < 75) {
                binding.glass.setImageResource(R.drawable.glass_50);
            } else if (percentCalc >= 75 && percentCalc < 100) {
                binding.glass.setImageResource(R.drawable.glass_75);
            } else if (percentCalc == 100) {
                binding.glass.setImageResource(R.drawable.glass_100);
            }

            incorrectCounter = 1;

        } else {
            incorrectCounter++;
            if (incorrectCounter <= MAX_ATTEMPTS) {
                Toast.makeText(getContext(), "Erġa' pprova!", Toast.LENGTH_SHORT).show();
                currentStage = Stage.PRE;
            } else {
                incorrectCounter = 1;
                Toast.makeText(getContext(), "Ippruvajt din il-kelma għal 5 darbiet", Toast.LENGTH_SHORT).show();
                Map<String, Object> wordAttempt = new HashMap<>();
                wordAttempt.put("word", currentWord);
                wordAttempt.put("attempts", "6");
                wordAttempt.put("listId", currentListId);
                wordAttempts.add(wordAttempt);

                int nextWordIndex = currentWordIndex + 1;
                int percentCalc = Math.round(((float) nextWordIndex / words.size()) * 100);
                String percent = percentCalc + "%";
                binding.progressText.setText(percent);

                if (percentCalc >= 25 && percentCalc < 50) {
                    binding.glass.setImageResource(R.drawable.glass_25);
                } else if (percentCalc >= 50 && percentCalc < 75) {
                    binding.glass.setImageResource(R.drawable.glass_50);
                } else if (percentCalc >= 75 && percentCalc < 100) {
                    binding.glass.setImageResource(R.drawable.glass_75);
                } else if (percentCalc == 100) {
                    binding.glass.setImageResource(R.drawable.glass_100);
                }

                doneStage();
                currentStage = Stage.PRE;

            }
        }


    }

    private boolean validateInput(String word) {
        List<Boolean> foundArray = new ArrayList<>();
        lettersList.clear();
        letters = word.toCharArray();
        for (char letter : letters) {
            lettersList.add(String.valueOf(letter));
        }
        //jien - lettersList - 4, letterInput 4 - comparing each letter with input in edit texts in same index position
        for (int i = 0; i < lettersList.size(); i++) {
            String expectedLetter = lettersList.get(i);
            String input = editTextList.get(i).getText().toString();
            ImageView tick = tickList.get(i);
            tick.setVisibility(View.VISIBLE);

            if (expectedLetter.equals(input)) {
                foundArray.add(true);
                editTextList.get(i).setFocusable(false);
                editTextList.get(i).setClickable(false);
                editTextList.get(i).setCursorVisible(false);
                editTextList.get(i).setBackgroundColor(Color.TRANSPARENT);
                editTextList.get(i).setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.correct));
            } else {
                foundArray.add(false);
                Drawable drawable = tick.getDrawable();
                drawable.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
                editTextList.get(i).setFocusable(false);
                editTextList.get(i).setClickable(false);
                editTextList.get(i).setCursorVisible(false);
                editTextList.get(i).setBackgroundColor(Color.TRANSPARENT);

            }
        }

        return !foundArray.contains(false);
    }

    private void doneStage() {
        currentWordIndex++;

        if (currentWordIndex < words.size()) {
            loadCurrentWord();
        } else {
            showCompletion();

        }
    }

    private void loadCurrentWord() {
        if (currentWordIndex >= words.size()) {
            showCompletion();
            return;
        }

        incorrectCounter = 1;


        resetUI();
        String currentWord = words.get(currentWordIndex);
        currentListId = listIdWordMap.get(currentWord);
        binding.textView.setText(currentWord);
        //synthesizeText(currentWord);
        highestStageReached = Stage.LOOK;
        binding.look.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.say.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.cover.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.write.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.check.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        updateButtonStates();

        currentStage = Stage.LOOK;
    }

    private void resetUI() {
        binding.letterContainer.removeAllViews();

        if (visualsManager.getVisualsType().equals(VisualsManager.VISUAL_1)) {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.VISIBLE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.GONE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);

        } else {
            binding.chestCheck.setVisibility(View.GONE);
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);

            binding.laptopClosed.setVisibility(View.VISIBLE);
            binding.laptopCheck.setVisibility(View.GONE);
            binding.laptopOpen.setVisibility(View.GONE);
        }


        binding.look.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.say.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.cover.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.write.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        binding.check.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDone));
        Drawable drawable = binding.imageView.getDrawable();
        Drawable drawable2 = binding.imageView2.getDrawable();
        Drawable drawable3 = binding.imageView3.getDrawable();
        Drawable drawable4 = binding.imageView4.getDrawable();
        Drawable drawable5 = binding.imageView5.getDrawable();

        drawable.setColorFilter(getResources().getColor(R.color.colorDone), PorterDuff.Mode.SRC_IN);
        drawable2.setColorFilter(getResources().getColor(R.color.colorDone), PorterDuff.Mode.SRC_IN);
        drawable3.setColorFilter(getResources().getColor(R.color.colorDone), PorterDuff.Mode.SRC_IN);
        drawable4.setColorFilter(getResources().getColor(R.color.colorDone), PorterDuff.Mode.SRC_IN);
        drawable5.setColorFilter(getResources().getColor(R.color.colorDone), PorterDuff.Mode.SRC_IN);


        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);
    }

    private void showCompletion() {
        binding.resultsContainer.setVisibility(View.VISIBLE);
        //binding.tickContainer.removeAllViews();
        binding.letterContainer.removeAllViews();

        binding.chestCheck.setVisibility(View.GONE);
        binding.chestClosed.setVisibility(View.GONE);
        binding.chestOpen.setVisibility(View.GONE);
        binding.laptopClosed.setVisibility(View.GONE);
        binding.laptopCheck.setVisibility(View.GONE);
        binding.laptopOpen.setVisibility(View.GONE);

        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);
        binding.stagesContainer.setVisibility(View.GONE);
        binding.progressContainer.setVisibility(View.GONE);
        mediaPlayer = MediaPlayer.create(getContext(), R.raw.complete);
        mediaPlayer.start();
        Toast.makeText(getContext(), "All words completed!", Toast.LENGTH_SHORT).show();

        // Group word attempts by list ID
        Map<String, List<Map<String, Object>>> attemptsByList = new HashMap<>();

        for (Map<String, Object> attempt : wordAttempts) {
            String listId = (String) attempt.get("listId");
            if (!attemptsByList.containsKey(listId)) {
                attemptsByList.put(listId, new ArrayList<>());
            }
            attemptsByList.get(listId).add(attempt);
            attempt.remove("listId");
        }
        if (currentUser != null) {
            String email = currentUser.getEmail();
            // For each list, fetch the list name and save attempts
            for (String listId : attemptsByList.keySet()) {
                List<Map<String, Object>> listAttempts = attemptsByList.get(listId);

                userViewModel.getRole().observe(getViewLifecycleOwner(), role -> {
                    if (role.equals("public")) {
                        userViewModel.getChildID().observe(getViewLifecycleOwner(), id -> {
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (id != null && currentUser != null) {
                                // Reference the specific child profile
                                DocumentReference childRef = db.collection("users")
                                        .document(currentUser.getUid())
                                        .collection("childProfiles")
                                        .document(id);

                                db.collection("users").document(currentUser.getUid())
                                        .collection("lists").document(listId)
                                        .get()
                                        .addOnCompleteListener(listTask -> {
                                            if (listTask.isSuccessful() && listTask.getResult().exists()) {
                                                String listName = listTask.getResult().getString("name");

                                                // Now save the attempts with the correct list name
                                                childRef.collection("statistics")
                                                        .whereEqualTo("name", listName)
                                                        .limit(1)
                                                        .get()
                                                        .addOnCompleteListener(statisticsTask -> {
                                                            // The rest of your existing code for saving attempts
                                                            DocumentReference statRef;
                                                            if (statisticsTask.isSuccessful() && !statisticsTask.getResult().isEmpty()) {
                                                                statRef = statisticsTask.getResult().getDocuments().get(0).getReference();
                                                            } else {
                                                                statRef = childRef.collection("statistics").document();
                                                                Map<String, Object> statData = new HashMap<>();
                                                                statData.put("name", listName);
                                                                statData.put("listId", listId);

                                                                statRef.set(statData);
                                                            }

                                                            // Add the attempt
                                                            Map<String, Object> attemptData = new HashMap<>();
                                                            attemptData.put("wordAttempts", listAttempts);
                                                            attemptData.put("completedAt", FieldValue.serverTimestamp());

                                                            statRef.collection("attempts")
                                                                    .add(attemptData)
                                                                    .addOnSuccessListener(aVoid -> {
                                                                        Log.d("Firestore", "Attempt saved successfully");
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.w("Firestore", "Error saving attempt", e);
                                                                    });
                                                        });
                                            }
                                        });
                            }
                        });

                    } else if (role.equals("student")) {
                        // Fetch the list name from the class's lists collection
                        db.collectionGroup("students")
                                .whereEqualTo("email", email)
                                .limit(1)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                        DocumentSnapshot studentDoc = task.getResult().getDocuments().get(0);
                                        String schoolId = studentDoc.getString("schoolId");
                                        String yearGroupId = studentDoc.getString("yearGroupId");
                                        String classRoomId = studentDoc.getString("classRoomId");

                                        // Get the list name using its ID
                                        db.collection("schools").document(schoolId)
                                                .collection("yearGroups").document(yearGroupId)
                                                .collection("classes").document(classRoomId)
                                                .collection("lists").document(listId)
                                                .get()
                                                .addOnCompleteListener(listTask -> {
                                                    if (listTask.isSuccessful() && listTask.getResult().exists()) {
                                                        String listName = listTask.getResult().getString("name");

                                                        // Now save the attempts with the correct list name
                                                        studentDoc.getReference().collection("statistics")
                                                                .whereEqualTo("name", listName)
                                                                .limit(1)
                                                                .get()
                                                                .addOnCompleteListener(statisticsTask -> {
                                                                    if (statisticsTask.isSuccessful()) {
                                                                        QuerySnapshot statisticsSnapshot = statisticsTask.getResult();
                                                                        DocumentReference statisticsDocRef;

                                                                        if (statisticsSnapshot != null && !statisticsSnapshot.isEmpty()) {
                                                                            statisticsDocRef = statisticsSnapshot.getDocuments().get(0).getReference();
                                                                        } else {
                                                                            // Create a new statistics document
                                                                            statisticsDocRef = studentDoc.getReference().collection("statistics").document();
                                                                            Map<String, Object> listDetails = new HashMap<>();
                                                                            listDetails.put("name", listName);
                                                                            listDetails.put("listId", listId);
                                                                            statisticsDocRef.set(listDetails);
                                                                        }

                                                                        // Add attempt to subcollection
                                                                        CollectionReference attemptsRef = statisticsDocRef.collection("attempts");

                                                                        Map<String, Object> data = new HashMap<>();
                                                                        data.put("wordAttempts", listAttempts);
                                                                        data.put("completedAt", FieldValue.serverTimestamp());

                                                                        attemptsRef.document()
                                                                                .set(data)
                                                                                .addOnSuccessListener(aVoid -> {
                                                                                    Log.d("Firestore", "Subcollection document created successfully");
                                                                                })
                                                                                .addOnFailureListener(e -> {
                                                                                    Log.w("Firestore", "Error creating subcollection document", e);
                                                                                });
                                                                    }
                                                                });
                                                    }
                                                });
                                    }
                                });
                    }
                });

            }
            binding.button.setOnClickListener(v -> {
                Navigation.findNavController(requireView()).popBackStack();

            });
        } else {
            binding.button.setOnClickListener(v -> {
                Navigation.findNavController(requireView()).popBackStack();
            });
        }

        LinearLayout wordColumn = binding.wordContainer;
        LinearLayout attemptsColumn = binding.attemptsContainer;
        allWords.clear();
        allAttempts.clear();

        for (int i = 0; i < wordAttempts.size(); i++) {
            TextView wordTextView = new TextView(getContext());
            TextView attemptTextView = new TextView(getContext());

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            FontManager fontManager = new FontManager(getContext());
            String fontType = fontManager.getFontType();

            applyFont(wordTextView, fontType);
            applyFont(attemptTextView, fontType);




            wordTextView.setLayoutParams(params);
            attemptTextView.setLayoutParams(params);

            String word = (String) wordAttempts.get(i).get("word");
            String attempt = (String) wordAttempts.get(i).get("attempts");

            if(attempt != null && attempt.equals("6")){
                attempt = "Erġa' pprova";
            }

            wordTextView.setText(word);
            wordTextView.setTextSize(20);
            attemptTextView.setText(attempt);
            attemptTextView.setTextSize(20);

            allAttempts.add(attemptTextView);
            attemptsColumn.addView(attemptTextView);
            allWords.add(wordTextView);
            wordColumn.addView(wordTextView);
        }


    }
    // Helper method to apply fonts
    private void applyFont(TextView textView, String fontType) {
        Typeface typeface;

        if (fontType.equals(FontManager.FONT_OPEN_DYSLEXIC)) {
            typeface = ResourcesCompat.getFont(getContext(), R.font.open_dyslexic);
        } else if (fontType.equals(FontManager.FONT_ANDIKA)) {
            typeface = ResourcesCompat.getFont(getContext(), R.font.andika);
        } else {
            // Default font
            typeface = ResourcesCompat.getFont(getContext(), R.font.comic_sans);
        }

        textView.setTypeface(typeface);
    }

    private void synthesizeText(String word) {
        try {
            SpeechConfig speechConfig = SpeechConfig.fromSubscription(SUBSCRIPTION_KEY, REGION);
            speechConfig.setSpeechSynthesisVoiceName("mt-MT-GraceNeural");

            SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig);

            // Wrap the word in SSML with a prosody rate
            String ssml = "<speak version='1.0' xml:lang='mt-MT'>" +
                    "<voice name='mt-MT-GraceNeural'>" +
                    "<prosody rate='slow'>" + word + "</prosody>" +
                    "</voice></speak>";

            SpeechSynthesisResult result = synthesizer.SpeakSsmlAsync(ssml).get();

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                Log.i("SpeechSynthesis", "Speech synthesized with adjusted rate for: " + word);
            } else if (result.getReason() == ResultReason.Canceled) {
                SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
                showError("Synthesis canceled: " + cancellation.getErrorDetails());
            }

            synthesizer.close();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error: " + e.getMessage());
        }
    }

    private void showError(String errorMessage) {
        if (getActivity() != null) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show());
        }
    }

    private void updateButtonStates() {
        binding.look.setEnabled(true);
        binding.say.setEnabled(highestStageReached.ordinal() >= Stage.SAY.ordinal());
        binding.cover.setEnabled(highestStageReached.ordinal() >= Stage.COVER.ordinal());
        binding.write.setEnabled(highestStageReached.ordinal() >= Stage.WRITE.ordinal());
        binding.check.setEnabled(highestStageReached.ordinal() >= Stage.CHECK.ordinal());
    }


}