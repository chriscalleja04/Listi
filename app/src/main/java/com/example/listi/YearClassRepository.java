package com.example.listi;

import static android.content.ContentValues.TAG;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YearClassRepository {
    private FirebaseFirestore db;
    private UserViewModel userViewModel;

    public YearClassRepository(UserViewModel userViewModel) {
        this.db = FirebaseFirestore.getInstance();
        this.userViewModel = userViewModel;
    }

    public void fetchYearGroups(String schoolID, String role, String email) {
        if (schoolID != null) {
            if (role.equals("admin")) {
                fetchAdminYearGroups(schoolID);
            } else if (role.equals("educator")) {
                fetchEducatorYearGroups(email, schoolID);
            }
        }
    }

    private void fetchAdminYearGroups(String schoolID) {
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> yearGroupNames = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                if (name != null) {
                                    yearGroupNames.add(name);
                                }
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                            userViewModel.setYearGroups(yearGroupNames);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public void fetchEducatorYearGroups(String email, String schoolID) {
        getEducatorYearGroupIds(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> yearGroupIds = task.getResult();
                if (yearGroupIds != null && !yearGroupIds.isEmpty()) {
                    fetchYearGroupsByIds(schoolID, yearGroupIds);
                }
            }
        });
    }

    private void fetchYearGroupsByIds(String schoolID, List<String> yearGroupIds) {
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .whereIn(FieldPath.documentId(), yearGroupIds)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> yearGroupNames = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                if (name != null) {
                                    yearGroupNames.add(name);
                                }
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                            userViewModel.setYearGroupsEducator(yearGroupNames);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public Task<List<String>> getEducatorYearGroupIds(String email) {
        TaskCompletionSource<List<String>> taskCompletionSource = new TaskCompletionSource<>();
        List<String> result = new ArrayList<>();
        db.collectionGroup("educators")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(educatorTask -> {
                    if (educatorTask.isSuccessful()) {
                        QuerySnapshot querySnapshot = educatorTask.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot doc : educatorTask.getResult()) {
                                String yearGroupId = doc.getString("yearGroupId");
                                if (yearGroupId != null) {
                                    result.add(yearGroupId);
                                }
                                Log.d(TAG, "Found educator in path: " + doc.getReference().getPath());
                            }
                            taskCompletionSource.setResult(result);
                        } else {
                            // No documents found
                            Log.d(TAG, "No educators found for email: " + email);
                            taskCompletionSource.setResult(result); // Return empty list
                        }
                    } else {
                        Log.e(TAG, "Error in educator query", educatorTask.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    public void fetchYearGroupID(String selectedYearGroup, LifecycleOwner lifecycleOwner) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID == null) {
            Log.e(TAG, "School ID is null");
            return;
        }
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .whereEqualTo("name", selectedYearGroup)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                String yearGroupID = querySnapshot.getDocuments().get(0).getId();
                                userViewModel.setYearGroupID(yearGroupID);

                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    String email = currentUser.getEmail();
                                    userViewModel.getRole().observe(lifecycleOwner, role -> {
                                        fetchClassRooms(yearGroupID, role, email);
                                    });
                                }
                                Log.d("Firestore", "Year Group ID: " + yearGroupID);

                            } else {
                                Log.d(TAG, "Doc does not exist");

                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public void fetchClassRooms(String yearGroupId, String role, String email) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID == null) {
            Log.e(TAG, "School ID is null");
            return;
        }
        if (role.equals("admin")) {
            fetchAdminClassRooms(schoolID, yearGroupId);
        } else if (role.equals("educator")) {
            fetchEducatorClassRooms(schoolID, yearGroupId, email);
        }
    }

    public void fetchAdminClassRooms(String schoolID, String yearGroupId) {
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupId)
                .collection("classes")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> classes = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                if (name != null) {
                                    classes.add(name);
                                }
                            }
                            userViewModel.setClasses(classes);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void fetchEducatorClassRooms(String schoolID, String yearGroupId, String email) {
        fetchEducatorClasses(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> classIds = task.getResult();
                if (classIds != null && !classIds.isEmpty()) {
                    db.collection("schools")
                            .document(schoolID)
                            .collection("yearGroups")
                            .document(yearGroupId)
                            .collection("classes")
                            .whereIn(FieldPath.documentId(), classIds)
                            .orderBy("name", Query.Direction.ASCENDING)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        List<String> classRoomNames = new ArrayList<>();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            String name = document.getString("name");
                                            if (name != null) {
                                                classRoomNames.add(name);
                                            }
                                            Log.d(TAG, document.getId() + " => " + document.getData());
                                        }
                                        userViewModel.setClassesEducator(classRoomNames);
                                    } else {
                                        Log.d(TAG, "Error getting documents: ", task.getException());
                                    }
                                }
                            });
                }
            }
        });
    }


    public Task<List<String>> fetchEducatorClasses(String email) {
        TaskCompletionSource<List<String>> taskCompletionSource = new TaskCompletionSource<>();
        List<String> result = new ArrayList<>();

        db.collectionGroup("educators")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(educatorTask -> {
                    if (educatorTask.isSuccessful()) {
                        QuerySnapshot querySnapshot = educatorTask.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            for (QueryDocumentSnapshot doc : educatorTask.getResult()) {
                                String classId = doc.getString("classRoomId");
                                if (classId != null) {
                                    result.add(classId);
                                }
                                Log.d(TAG, "Found educator in path: " + doc.getReference().getPath());
                            }
                            taskCompletionSource.setResult(result);
                        } else {
                            // No documents found
                            Log.d(TAG, "No educators found for email: " + email);
                            taskCompletionSource.setResult(result); // Return empty list
                        }
                    } else {
                        Log.e(TAG, "Error in educator query", educatorTask.getException());
                    }
                });

        return taskCompletionSource.getTask();
    }

    public Task<String> fetchClassRoomIDByName(String selectedClassRoom) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupID = userViewModel.getYearGroupID().getValue();

        if (schoolID == null || yearGroupID == null) {
            taskCompletionSource.setException(new Exception("School ID or Year Group is null"));
            return taskCompletionSource.getTask();
        }
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupID)
                .collection("classes")
                .whereEqualTo("name", selectedClassRoom)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                String classRoomID = querySnapshot.getDocuments().get(0).getId();
                                userViewModel.setClassRoomID(classRoomID);
                                taskCompletionSource.setResult(classRoomID);
                            } else {
                                Log.d(TAG, "Name does not exist");
                                taskCompletionSource.setException(new Exception("Classroom name does not exist")); // Or handle no class found differently
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            taskCompletionSource.setException(task.getException());
                        }
                    }
                });
        return taskCompletionSource.getTask();
    }

    public void fetchSchoolName(String schoolID) {
        if (schoolID != null) {
            db.collection("schools")
                    .document(schoolID)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String name = document.getString("name");
                                    userViewModel.setSchoolName(name);
                                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                } else {
                                    Log.d(TAG, "No such document");
                                }
                            } else {
                                Log.d(TAG, "get failed with ", task.getException());
                            }
                        }
                    });
        }
    }


    public void saveNewEducator(String name, String email, String selectedClassRoomName, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupID = userViewModel.getYearGroupID().getValue();

        if (schoolID == null || yearGroupID == null) {
            onFailure.onFailure(new Exception("School ID or Year Group ID is null"));
            return;
        }

        String role = "educator";
        Map<String, Object> educatorDetails = new HashMap<>();
        educatorDetails.put("name", name);
        educatorDetails.put("email", email);
        educatorDetails.put("role", role);
        educatorDetails.put("schoolId", schoolID);
        educatorDetails.put("yearGroupId", yearGroupID);

        fetchClassRoomIDByName(selectedClassRoomName).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String classRoomID = task.getResult();
                educatorDetails.put("classRoomId", classRoomID);

                db.collection("schools")
                        .document(schoolID)
                        .collection("yearGroups")
                        .document(yearGroupID)
                        .collection("classes")
                        .document(classRoomID)
                        .collection("educators")
                        .add(educatorDetails)
                        .addOnSuccessListener(documentReference -> {
                            updateUserRecord(email, name, role, schoolID, onSuccess, onFailure);
                        })
                        .addOnFailureListener(onFailure);
            } else {
                onFailure.onFailure(task.getException());
            }
        });

    }

    private void updateUserRecord(String email, String name, String role, String schoolID, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (!querySnapshot.isEmpty()) {
                                String userId = querySnapshot.getDocuments().get(0).getId();
                                Map<String, Object> userDetails = new HashMap<>();
                                userDetails.put("email", email);
                                userDetails.put("name", name);
                                userDetails.put("role", role);
                                userDetails.put("schoolId", schoolID);

                                db.collection("users")
                                        .document(userId)
                                        .set(userDetails)
                                        .addOnSuccessListener(onSuccess)
                                        .addOnFailureListener(onFailure);
                            } else {
                                onSuccess.onSuccess(null);
                            }
                        } else {
                            onFailure.onFailure(task.getException());
                        }
                    }

                });
    }
}




