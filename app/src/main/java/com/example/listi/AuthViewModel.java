package com.example.listi;

import android.app.Activity;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.content.ContentValues.TAG;


public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final MutableLiveData<FirebaseUser> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> authMessage = new MutableLiveData<>();

    private final MutableLiveData<String> userRole = new MutableLiveData<>();

    private final MutableLiveData<String> schoolId = new MutableLiveData<>();

    public AuthViewModel(){
        authRepository = new AuthRepository();
        currentUser.setValue(authRepository.getCurrentUser());
    }

    public LiveData<FirebaseUser> getCurrentUser(){
        return currentUser;
    }

    public LiveData<Boolean> getIsLoading(){
        return isLoading;
    }

    public LiveData<String> getAuthMessage(){
        return authMessage;
    }

    public LiveData<String> getUserRole(){
        return userRole;
    }

    public LiveData<String> getSchoolID(){
        return schoolId;
    }

    public void loginWithMicrosoft(Activity activity){
        isLoading.setValue(true);
        authRepository.loginWithMicrosoft(activity)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    isLoading.setValue(false);
                    currentUser.setValue(authResult.getUser());

                    authRepository.addUserToFirestore(user)
                        .addOnSuccessListener(aVoid ->{
                            fetchUserRole(user.getUid());
                            authMessage.setValue("Sign in successful");
                        })
                        .addOnFailureListener(e->{
                            Log.e(TAG, "Failed to Add User to Firestore", e);
                            authMessage.setValue("Sign in successful but failed to save user data");
                        });

                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    authMessage.setValue("Sign in failed: " + e.getMessage());
                });
    }

    public Task<AuthResult> loginWithEmailPassword(String email, String password){
        isLoading.setValue(true);
        return authRepository.loginWithEmailPassword(email, password)
                .addOnCompleteListener(task -> {
                        isLoading.setValue(false);
                        if(task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            currentUser.setValue(user);
                            fetchUserRole(user.getUid());
                            authMessage.setValue("Sign in successful");
                        } else {
                            authMessage.setValue("Sign in failed: "+task.getException().getMessage());
                    };
                });
    }

    public Task<AuthResult> registerWithEmailPassword(String email, String password, String displayName){
        isLoading.setValue(true);
        return authRepository.registerWithEmailPassword(email,password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        FirebaseUser user = task.getResult().getUser();
                        authRepository.updateUserProfile(user, displayName)
                                .addOnCompleteListener(profileTask ->{
                                    authRepository.addUserToFirestore(user)
                                            .addOnSuccessListener(aVoid -> {
                                                currentUser.setValue(user);
                                                fetchUserRole(user.getUid());
                                                isLoading.setValue(false);
                                                authMessage.setValue("Registration Successful");
                                            })
                                            .addOnFailureListener(e -> {
                                                isLoading.setValue(false);
                                                Log.e(TAG, "Failed to add user to firestore", e);
                                                authMessage.setValue("Registration successful but failed to save user data");
                                            });
                                });
                    }else {
                        isLoading.setValue(false);
                        authMessage.setValue("Registration Failed "+ task.getException().getMessage());
                    }
                });
    }




    public void signOut(){
        authRepository.signOut();
        currentUser.setValue(null);
        userRole.setValue(null);
        schoolId.setValue(null);
    }

    public void setUser(FirebaseUser user){
        currentUser.setValue(user);
        if(user!=null){
            fetchUserRole(user.getUid());
        }
    }

    public void clearAuthMessage(){
        authMessage.setValue("");
    }

    public void fetchUserRole(String userId){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        String role = documentSnapshot.getString("role");
                        String school = documentSnapshot.getString("school");
                        userRole.setValue(role);
                        schoolId.setValue(school);
                    }else{
                        userRole.setValue("public");
                        schoolId.setValue(null);
                    }
                })
                .addOnFailureListener(e ->{
                    Log.e(TAG, "Error fetching user role", e);
                    userRole.setValue("public");
                    schoolId.setValue(null);
                });
    }
}
