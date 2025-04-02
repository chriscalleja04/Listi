package com.example.listi;

public class Student {
    private String id,name, email;

    public Student() {}
    public Student(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
