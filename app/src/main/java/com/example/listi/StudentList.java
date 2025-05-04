package com.example.listi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentList {
    public StudentList(){}

    public StudentList(String id, String name, List<Map<String,Object>> wordAttempts) {
        this.id = id;
        this.name = name;
        this.wordAttempts = wordAttempts;

    }

    String id;
    String name;
    List<Map<String,Object>> wordAttempts;


    public String getId() {
        return id;

    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
