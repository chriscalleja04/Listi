package com.example.listi;

import java.util.ArrayList;

public class YearGroup {
    private String id,name;

    public YearGroup() {}
    public YearGroup(String id, String name) {
        this.id = id;
        this.name = name;

    }

    public String getID(){
        return id;
    }
    public String getName() {
        return name;
    }


    public void setID(String id){
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }


}
