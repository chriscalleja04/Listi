package com.example.listi;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;

public class UserViewModel extends ViewModel {
    private final MutableLiveData<FirebaseUser> user = new MutableLiveData<>();

    public void setUser(FirebaseUser user){
        this.user.setValue(user);
    }

    public LiveData<FirebaseUser> getUser(){
        return user;
    }
}
