package com.example.listi;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();

    private final MutableLiveData<String> userIdEducator = new MutableLiveData<>();
    private final MutableLiveData<String> role = new MutableLiveData<>();
    private final MutableLiveData<List<String>> yearGroups = new MutableLiveData<>();
    private final MutableLiveData<List<String>> yearGroupsEducator = new MutableLiveData<>();
    private final MutableLiveData<List<String>> classes = new MutableLiveData<>();
    private final MutableLiveData<List<String>> classesEducator = new MutableLiveData<>();
    private final MutableLiveData<String> schoolName = new MutableLiveData<>();
    private final MutableLiveData<String> schoolID = new MutableLiveData<>();
    private final MutableLiveData<String> yearGroupID = new MutableLiveData<>();
    private final MutableLiveData<String> classRoomID = new MutableLiveData<>();

    public void setUser(FirebaseUser user){
        this.user.setValue(user);
    }



    public void setRole(String role){
        this.role.setValue(role);
    }
    public void setSchoolName(String schoolName){
        this.schoolName.setValue(schoolName);
    }

    public void setSchoolID(String schoolID){
        this.schoolID.setValue(schoolID);
    }


    public void setYearGroupID(String yearGroupID){
        this.yearGroupID.setValue(yearGroupID);
    }

    public void setClassRoomID(String classRoomID){
        this.classRoomID.setValue(classRoomID);
    }

    public void setYearGroups(List<String> yearGroups){
        this.yearGroups.setValue(yearGroups);
    }

    public void setYearGroupsEducator(List<String> yearGroups){
        this.yearGroupsEducator.setValue(yearGroups);
    }

    public void setClasses(List<String> classes){
        this.classes.setValue(classes);
    }

    public void setClassesEducator(List<String> classesEducator){
        this.classesEducator.setValue(classesEducator);
    }

    public LiveData<FirebaseUser> getUser(){
        return user;
    }

    public LiveData<String> getUserIdEducator(){
        return userIdEducator;
    }

    public LiveData<String> getRole(){
        return role;
    }

    public LiveData<String> getSchoolName(){
        return schoolName;
    }

    public LiveData<List<String>> getYearGroups(){
        return yearGroups;
    }

    public LiveData<List<String>> getYearGroupsEducator(){
        return yearGroupsEducator;
    }

    public LiveData<List<String>> getClasses(){
        return classes;
    }

    public LiveData<List<String>> getClassesEducator(){
        return classesEducator;
    }


    public LiveData<String> getSchoolID(){
        return schoolID;
    }

    public LiveData<String> getYearGroupID(){
        return yearGroupID;
    }

    public LiveData<String> getClassRoomID(){
        return classRoomID;
    }




}
