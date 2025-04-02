package com.example.listi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class WordList {
    String id;
    String name;
    ArrayList<String> words;

    boolean isChecked = false;

    public WordList(){}

    public WordList(String id, String name, ArrayList<String> words, Boolean isChecked) {
        this.id = id;
        this.name = name;
        this.words = words;
        this.isChecked = isChecked;

    }


    public String getId() {
        return id;

    }

    public void setId(String id) {
        this.id = id;
    }
    public ArrayList<String> getWords() {
        return words;
    }

    public void setWords(ArrayList<String> words) {
        this.words = words;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isChecked(){
        return isChecked;
    }

    public void setChecked(boolean isChecked){
        this.isChecked = isChecked;
    }
}
