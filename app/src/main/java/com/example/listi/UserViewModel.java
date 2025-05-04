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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();

    private final MutableLiveData<String> userIdEducator = new MutableLiveData<>();
    private final MutableLiveData<String> role = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<YearGroup>> yearGroups = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<YearGroup>> yearGroupsEducator = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<ClassRoom>> classes = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<ClassRoom>> classesEducator = new MutableLiveData<>();

    private final MutableLiveData<ArrayList<Student>> students = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<Educator>> educators = new MutableLiveData<>();

    private final MutableLiveData<ArrayList<WordList>> lists = new MutableLiveData<>();

    public final MutableLiveData<ArrayList<ChildProfile>> childProfiles = new MutableLiveData<>();

    private final MutableLiveData<String> schoolName = new MutableLiveData<>();
    private final MutableLiveData<String> schoolID = new MutableLiveData<>();
    private final MutableLiveData<String> yearGroupID = new MutableLiveData<>();
    private final MutableLiveData<String> classRoomID = new MutableLiveData<>();


    private final MutableLiveData<String> childID = new MutableLiveData<>();
    private final MutableLiveData<String> childName = new MutableLiveData<>();

    private final MutableLiveData<Map<String, List<String>>> incorrectWords = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isNetworkAvailable = new MutableLiveData<>();


    public void setUser(FirebaseUser user){
        this.user.setValue(user);
    }
    public void setRole(String role){
        Log.d("UserViewModel", "Setting role to: " + role);

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

    public void setYearGroups(ArrayList<YearGroup> yearGroups){
        this.yearGroups.setValue(yearGroups);
    }

    public void setYearGroupsEducator(ArrayList<YearGroup> yearGroups){
        this.yearGroupsEducator.setValue(yearGroups);
    }

    public void setClasses(ArrayList<ClassRoom> classes){
        this.classes.setValue(classes);
    }

    public void setClassesEducator(ArrayList<ClassRoom> classesEducator){
        this.classesEducator.setValue(classesEducator);
    }

    public void setStudents(ArrayList<Student> students){
        this.students.setValue(students);
    }

    public void setEducators(ArrayList<Educator> educators){
        this.educators.setValue(educators);
    }

    public void setLists(ArrayList<WordList> lists) {this.lists.setValue(lists);}

    public void setChildProfiles(ArrayList<ChildProfile> childProfiles){ this.childProfiles.setValue(childProfiles);}


    public void setChildID(String childID){
        this.childID.setValue(childID);
    }

    public void setChildName(String childName){
        this.childName.setValue(childName);
    }

    public void setIncorrectWords(Map<String, List<String>> incorrectWords){this.incorrectWords.setValue(incorrectWords);}

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

    public LiveData<ArrayList<YearGroup>> getYearGroups(){
        return yearGroups;
    }

    public LiveData<ArrayList<YearGroup>> getYearGroupsEducator(){
        return yearGroupsEducator;
    }

    public LiveData<ArrayList<ClassRoom>> getClasses(){
        return classes;
    }

    public LiveData<ArrayList<ClassRoom>>getClassesEducator(){
        return classesEducator;
    }

    public LiveData<ArrayList<Student>>getStudents(){
        return students;
    }

    public LiveData<ArrayList<Educator>>getEducators(){
        return educators;
    }

    public LiveData<ArrayList<WordList>>getLists(){return lists;}
    public LiveData<ArrayList<ChildProfile>>getChildProfiles(){ return childProfiles; }




    public LiveData<Boolean> getIsNetworkAvailable() {
        return isNetworkAvailable;
    }

    public void setNetworkAvailable(boolean isAvailable) {
        isNetworkAvailable.setValue(isAvailable);
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

    public LiveData<String> getChildID() {return childID;}

    public LiveData<String> getChildName() {return childName;}

    public LiveData<Map<String, List<String>>> getIncorrectWords(){return incorrectWords;}


}
