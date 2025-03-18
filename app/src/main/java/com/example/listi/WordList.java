package com.example.listi;

import java.util.ArrayList;
import java.util.List;

public class WordList {

    public WordList(){}

    public WordList(String id, String name, ArrayList<String> words) {
        this.id = id;
        this.name = name;
        this.words = words;

    }

    String id;
    String name;
    ArrayList<String> words;


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
}
