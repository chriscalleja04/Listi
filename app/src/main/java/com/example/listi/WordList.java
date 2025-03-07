package com.example.listi;

import java.util.ArrayList;
import java.util.List;

public class WordList {

    public WordList(){}

    public WordList(String name, ArrayList<String> words) {
        this.name = name;
        this.words = words;
    }

    String name;
    ArrayList<String> words;


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
}
