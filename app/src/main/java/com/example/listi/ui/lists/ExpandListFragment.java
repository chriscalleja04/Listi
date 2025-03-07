package com.example.listi.ui.lists;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.KeyEvent;
import android.widget.Toast;


import com.example.listi.AzureTTSHelper;
import com.example.listi.R;
import com.example.listi.databinding.FragmentExpandListBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ExpandListFragment extends Fragment {
    private FragmentExpandListBinding binding;
    private List<EditText> editTextList = new ArrayList<>();
    private char[] letters;
    private List<String> lettersList = new ArrayList<>();

    private List<String> words;
    private int currentWordIndex = 0;
    private final int MAX_ATTEMPTS = 5;
    private int incorrectCounter = 0;

    private enum Stage {PRE, LOOK, SAY, COVER, WRITE, CHECK, DONE};

    private Stage currentStage = Stage.LOOK;
    private Stage highestStageReached = Stage.LOOK;


    private static final String SUBSCRIPTION_KEY = "NC0lur3fQY1ba9oEOGMxUWDZf7aD6jw0KkI5b5nXdSuWAzoh1BYBJQQJ99BBACfhMk5XJ3w3AAAAACOG5tI5";
    private static final String REGION = "swedencentral";

    private File cachedAudioFile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentExpandListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.chestClosed.setVisibility(View.VISIBLE);
        binding.chestCheck.setVisibility(View.GONE);
        binding.chestOpen.setVisibility(View.GONE);
        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);
        binding.look.setOnClickListener(v->{
            lookStage();
        });
        binding.say.setOnClickListener(v->{
            sayStage();
        });
        binding.cover.setOnClickListener(v->{
            coverStage();
        });
        binding.write.setOnClickListener(v->{
            writeStage();
        });
        binding.check.setOnClickListener(v->{
            checkStage();
        });
        words = getArguments() != null ? getArguments().getStringArrayList("words") : new ArrayList<>();

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

    private void preStage(){
        binding.letterContainer.removeAllViews();
        binding.chestCheck.setVisibility(View.GONE);
        binding.chestClosed.setVisibility(View.VISIBLE);
        binding.chestOpen.setVisibility(View.GONE);
        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);

        currentStage = Stage.LOOK;
    }
    private void lookStage() {
        binding.letterContainer.removeAllViews();

        String currentWord = words.get(currentWordIndex);
        binding.textView.setText(currentWord);
        binding.chestCheck.setVisibility(ViewGroup.GONE);
        binding.chestClosed.setVisibility(View.GONE);
        binding.chestOpen.setVisibility(View.VISIBLE);
        binding.textView.setVisibility(View.VISIBLE);
        binding.speaker.setVisibility(View.GONE);

        if (highestStageReached.ordinal() < Stage.SAY.ordinal()) {
            binding.look.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            highestStageReached = Stage.LOOK;
        }
        currentStage = Stage.SAY;
        updateButtonStates();
    }

    public void sayStage() {
        binding.chestOpen.setVisibility(View.VISIBLE);
        binding.chestClosed.setVisibility(View.GONE);
        binding.textView.setVisibility(View.VISIBLE);
        binding.speaker.setVisibility(View.VISIBLE);
        binding.speaker.setOnClickListener(v -> {
            if(cachedAudioFile != null && cachedAudioFile.exists()){
                playAudio(cachedAudioFile);
            }
        });

        if (highestStageReached.ordinal() < Stage.SAY.ordinal()) {
            binding.say.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            highestStageReached = Stage.SAY;
        }

        currentStage = Stage.COVER;
        updateButtonStates();


    }

    public void coverStage() {
        binding.letterContainer.removeAllViews();
        binding.chestClosed.setVisibility(View.VISIBLE);
        binding.chestOpen.setVisibility(View.GONE);
        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);


        if (highestStageReached.ordinal() < Stage.COVER.ordinal()) {
            binding.cover.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            highestStageReached = Stage.COVER;
        }

        currentStage = Stage.WRITE;
    }

    public void writeStage() {
        LinearLayout letterContainer = binding.letterContainer;
        letterContainer.removeAllViews();
        editTextList.clear();

        String word = words.get(currentWordIndex);
        LinearLayout editTextRow = new LinearLayout(getContext());
        editTextRow.setOrientation(LinearLayout.HORIZONTAL);
        editTextRow.setGravity(Gravity.CENTER);

        editTextList.clear();

        for (int i = 0; i < word.length(); i++) {
            // Create EditText for each letter
            EditText letterInput = new androidx.appcompat.widget.AppCompatEditText(getContext());
            letterInput.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            letterInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            letterInput.setAutofillHints(String.valueOf(View.AUTOFILL_TYPE_NONE));
            letterInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)}); // Only 1 char
            letterInput.setEms(1);
            letterInput.setTextSize(18);
            //letterInput.setGravity(Gravity.CENTER);
            letterInput.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            // In your EditText setup, modify the OnFocusChangeListener:
            letterInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        // Allow focusing on the field but control cursor position
                        letterInput.setSelection(letterInput.getText().length()); // Put cursor at the end of any existing text
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
            final int index = i; //for text watcher
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
            // Add EditText and TextView to their respective rows
            editTextRow.addView(letterInput);
        }

        // Add both rows to the main container AFTER the loop
        letterContainer.addView(editTextRow);

        binding.chestClosed.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);
        binding.chestOpen.setVisibility(View.VISIBLE);
        binding.textView.setVisibility(View.GONE);

        if (highestStageReached.ordinal() < Stage.WRITE.ordinal()) {
            binding.write.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            highestStageReached = Stage.WRITE;
        }

        currentStage = Stage.CHECK;
        updateButtonStates();

    }

    private void checkStage() {
        String currentWord = words.get(currentWordIndex);
        boolean isCorrect = validateInput(currentWord);

        if (isCorrect) {
            binding.chestClosed.setVisibility(View.GONE);
            binding.chestCheck.setVisibility(View.VISIBLE);
            binding.speaker.setVisibility(View.GONE);
            binding.chestOpen.setVisibility(View.GONE);
            binding.textView.setVisibility(View.GONE);

            incorrectCounter = 0;
            if (highestStageReached.ordinal() < Stage.CHECK.ordinal()) {
                binding.check.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
                binding.look.setEnabled(false);
                binding.say.setEnabled(false);
                binding.cover.setEnabled(false);
                binding.write.setEnabled(false);
                binding.check.setEnabled(false);
                highestStageReached = Stage.CHECK;
            }

            currentStage = Stage.DONE;


        } else {
            incorrectCounter++;
            if(incorrectCounter<MAX_ATTEMPTS) {
                Toast.makeText(getContext(), "Erġa pprova!", Toast.LENGTH_SHORT).show();
                currentStage = Stage.PRE;
            }else{
                incorrectCounter = 0;
                Toast.makeText(getContext(), "Mexxiiii!", Toast.LENGTH_SHORT).show();
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
            if (expectedLetter.equals(input)) {
                foundArray.add(true);
                editTextList.get(i).setFocusable(false);
                editTextList.get(i).setClickable(false);
                editTextList.get(i).setCursorVisible(false);
                editTextList.get(i).setBackgroundColor(Color.TRANSPARENT);
                editTextList.get(i).setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.correct));            } else {
                foundArray.add(false);
                editTextList.get(i).setFocusable(false);
                editTextList.get(i).setClickable(false);
                editTextList.get(i).setCursorVisible(false);
                editTextList.get(i).setBackgroundColor(Color.TRANSPARENT);

            }
        }

        if (!foundArray.contains(false)) {

            return true;
        } else {

            return false;
        }
        }

        private void doneStage(){
            currentWordIndex++;
            if (currentWordIndex < words.size()) {
                loadCurrentWord();
            }else{
                showCompletion();

            }
        }

        private void loadCurrentWord() {
            if(currentWordIndex >= words.size()) {
                showCompletion();
                return;
            }

            incorrectCounter = 0;

            resetUI();
            String currentWord = words.get(currentWordIndex);
            binding.textView.setText(currentWord);
            synthesizeText(currentWord);
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
        binding.chestCheck.setVisibility(View.GONE);
        binding.chestClosed.setVisibility(View.VISIBLE);
        binding.chestOpen.setVisibility(View.GONE);
        binding.textView.setVisibility(View.GONE);
        binding.speaker.setVisibility(View.GONE);
    }

    private void showCompletion() {
        Toast.makeText(getContext(), "All words completed!", Toast.LENGTH_SHORT).show();
    }
    private void synthesizeText(String word) {
        AzureTTSHelper ttsHelper = new AzureTTSHelper(SUBSCRIPTION_KEY, REGION);

        ttsHelper.synthesizeSpeech(word, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && getContext() != null) {
                    byte[] audioData = response.body().bytes();

                    // Cache the audio file
                    try {
                        cachedAudioFile = File.createTempFile("azure_tts", ".wav", requireContext().getCacheDir());
                        FileOutputStream fos = new FileOutputStream(cachedAudioFile);
                        fos.write(audioData);
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    private void playAudio(File audioFile) {
            requireActivity().runOnUiThread(() -> {
                MediaPlayer mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }
    private void updateButtonStates() {
        binding.look.setEnabled(true);
        binding.say.setEnabled(highestStageReached.ordinal() >= Stage.SAY.ordinal());
        binding.cover.setEnabled(highestStageReached.ordinal() >= Stage.COVER.ordinal());
        binding.write.setEnabled(highestStageReached.ordinal() >= Stage.WRITE.ordinal());
        binding.check.setEnabled(highestStageReached.ordinal() >= Stage.CHECK.ordinal());
    }


}