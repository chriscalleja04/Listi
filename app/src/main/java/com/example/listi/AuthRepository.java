package com.example.listi;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private final FirebaseAuth firebaseAuth;
    private final OAuthProvider.Builder microsoftProvider;

    private final FirebaseFirestore db;

    public AuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.microsoftProvider = OAuthProvider.newBuilder("microsoft.com")
                .addCustomParameter("ui_locales", "mt-MT")
                .addCustomParameter("prompt", "select_account")
                .addCustomParameter("tenant", "common")
                .addCustomParameter("mkt", "mt-MT")
                .addCustomParameter("locale", "mt-MT");
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseUser getCurrentUser(){
        return firebaseAuth.getCurrentUser();
    }

    public Task<AuthResult> loginWithMicrosoft(Activity activity){
        Task<AuthResult> pendingResultTask = firebaseAuth.getPendingAuthResult();
        if(pendingResultTask != null){
            Log.d(TAG, "loginWithMicrosoft: pending auth found");
            return pendingResultTask;
        } else {
            Log.d(TAG, "loginWithMicrosoft: starting new auth flow");
            return firebaseAuth.startActivityForSignInWithProvider(activity,microsoftProvider.build());
        }
    }

    public Task<AuthResult> loginWithEmailPassword(String email, String password){
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> registerWithEmailPassword(String email, String password){
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<Void> updateUserProfile (FirebaseUser user, String displayName){
        UserProfileChangeRequest profileUpdates =  new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();
        return user.updateProfile(profileUpdates);
    }
    public void signOut(){
        firebaseAuth.signOut();
    }

    public void addAuthStateListener(FirebaseAuth.AuthStateListener listener){
        firebaseAuth.addAuthStateListener(listener);
    }

    public void removeAuthStateListener(FirebaseAuth.AuthStateListener listener){
        firebaseAuth.removeAuthStateListener(listener);
    }

    public Task<Void> addUserToFirestore(FirebaseUser user){
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();

        if(user!=null){
            String uid = user.getUid();
            String email = user.getEmail();
            String name = user.getDisplayName();

            // Check if the user is an educator
            db.collection("users").document(uid).get()
                .addOnCompleteListener(task ->{
                    if(task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            Log.d(TAG, "User already exists in db");
                            taskCompletionSource.setResult(null);
                        }else{
                            checkRole(email).addOnCompleteListener(roleTask -> {
                                if (roleTask.isSuccessful()) {
                                    Map<String, String> schoolAndRole = roleTask.getResult();
                                    String schoolId = schoolAndRole.get("schoolId");
                                    String role = schoolAndRole.get("role");
                                    // Create user data
                                    Map<String, Object> userDetails = new HashMap<>();
                                    userDetails.put("email", email);
                                    userDetails.put("name", name);
                                    userDetails.put("role", role);
                                    userDetails.put("schoolId", schoolId);
                                    // Add user to Firestore
                                    db.collection("users")
                                            .document(uid)
                                            .set(userDetails)
                                            .addOnSuccessListener(unused -> {
                                                Log.d(TAG, "User added to Firestore");
                                                taskCompletionSource.setResult(null);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "Error adding user to Firestore", e);
                                                taskCompletionSource.setException(e);
                                            });
                                }else{
                                    Log.w(TAG, "Error checking educator status", roleTask.getException());
                                    taskCompletionSource.setException(roleTask.getException());
                                }
                            });
                        }
                    }else{
                        Log.w(TAG, "Error checking if user exists", task.getException());
                        taskCompletionSource.setException(task.getException());
                    }
                });
                }else {
                    taskCompletionSource.setException(new IllegalArgumentException("User Cannot be null"));
                }
                return taskCompletionSource.getTask();
             }

    public Task<Map<String, String>> checkRole(String email) {
        TaskCompletionSource<Map<String, String>> taskCompletionSource = new TaskCompletionSource<>();
        Map<String, String> result = new HashMap<>();

        db.collectionGroup("educators")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnCompleteListener(educatorTask -> {
                    if (educatorTask.isSuccessful()) {
                        QuerySnapshot educatorSnapshot = educatorTask.getResult();
                        if (educatorSnapshot != null && !educatorSnapshot.isEmpty()) {

                            DocumentSnapshot doc = educatorSnapshot.getDocuments().get(0);
                            String schoolID = doc.getString("schoolId");
                            String role = doc.getString("role");
                            result.put("schoolId", schoolID);
                            result.put("role", role);
                            Log.d(TAG, "Found educator in path: " + doc.getReference().getPath());
                            taskCompletionSource.setResult(result);
                        } else {
                            try {
                                // No educator found, check students
                                db.collectionGroup("students")
                                        .whereEqualTo("email", email)
                                        .limit(1)
                                        .get()
                                        .addOnCompleteListener(studentTask -> {
                                            if (studentTask.isSuccessful()) {
                                                QuerySnapshot studentSnapshot = studentTask.getResult();
                                                if (studentSnapshot != null && !studentSnapshot.isEmpty()) {

                                                    DocumentSnapshot doc = studentSnapshot.getDocuments().get(0);
                                                    String schoolId = doc.getString("schoolId");
                                                    String role = doc.getString("role");
                                                    result.put("schoolId", schoolId);
                                                    result.put("role", role);
                                                    Log.d(TAG, "Found student in path: " + doc.getReference().getPath());
                                                } else {
                                                    Log.d(TAG, "User not found in educators or students");
                                                    result.put("schoolId", null);
                                                    result.put("role", "public");
                                                }
                                                taskCompletionSource.setResult(result);
                                            } else {
                                                Log.e(TAG, "Error in student query, defaulting to public user", studentTask.getException());
                                                result.put("schoolId", null);
                                                result.put("role", "public");
                                                taskCompletionSource.setResult(result);
                                            }
                                        });
                            } catch (Exception e) {
                                Log.e(TAG, "Exception when checking student status, defaulting to public user", e);
                                result.put("schoolId", null);
                                result.put("role", "public");
                                taskCompletionSource.setResult(result);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error in educator query, defaulting to public user", educatorTask.getException());
                        result.put("schoolId", null);
                        result.put("role", "public");
                        taskCompletionSource.setResult(result);
                    }
                });

        return taskCompletionSource.getTask();
    }

}
