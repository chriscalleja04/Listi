package com.example.listi;

import static android.content.ContentValues.TAG;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class YearClassRepository {
    private FirebaseFirestore db;
    private UserViewModel userViewModel;

    private ArrayList<YearGroup> yearGroupsListAdmin = new ArrayList<>();
    private ArrayList<YearGroup> yearGroupsListEducator = new ArrayList<>();

    private ArrayList<ClassRoom> classRoomArrayListAdmin = new ArrayList<>();

    private ArrayList<ClassRoom> classRoomArrayListEducator = new ArrayList<>();

    private ArrayList<Student> studentArrayList = new ArrayList<>();

    private ArrayList<Educator> educatorArrayList = new ArrayList<>();

    private ArrayList<WordList> listArrayList = new ArrayList<>();

    private ArrayList<WordList> listArrayListParent = new ArrayList<>();

    private ArrayList<ChildProfile> childProfilesList = new ArrayList<>();

   private Map<String, List<String>> incorrectWordsByList = new HashMap<>();


    private MyYearGroupsAdapter mYearGroupsAdapter;


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
        yearGroupsListAdmin.clear();
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            // Handle Firestore errors
                            Log.e("Firestore error", error.getMessage());
                            return;
                        }

                        // Check if the QuerySnapshot is null or empty
                        if (value == null || value.isEmpty()) {
                            Log.d(TAG, "No documents found in the query.");
                            return;
                        }

                        // Process document changes
                        for (DocumentChange document : value.getDocumentChanges()) {
                            if (document.getType() == DocumentChange.Type.ADDED) {
                                YearGroup yearGroup = document.getDocument().toObject(YearGroup.class);
                                yearGroup.setID(document.getDocument().getId());
                                yearGroupsListAdmin.add(yearGroup);

                            }
                        }
                        userViewModel.setYearGroups(yearGroupsListAdmin);
                    }
                });
    }

    public void fetchEducatorYearGroups(String email, String schoolID) {
        getEducatorYearGroupIds(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<String> yearGroupIds = task.getResult();
                if (yearGroupIds != null && !yearGroupIds.isEmpty()) {
                    fetchYearGroupsByIds(schoolID, yearGroupIds);
                }
            }
        });
    }


    private void fetchYearGroupsByIds(String schoolID, ArrayList<String> yearGroupIds) {
        yearGroupsListEducator.clear();
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .whereIn(FieldPath.documentId(), yearGroupIds)
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            // Handle Firestore errors
                            Log.e("Firestore error", error.getMessage());
                            return;
                        }

                        // Check if the QuerySnapshot is null or empty
                        if (value == null || value.isEmpty()) {
                            Log.d(TAG, "No documents found in the query.");
                            return;
                        }

                        // Process document changes
                        for (DocumentChange document : value.getDocumentChanges()) {
                            if (document.getType() == DocumentChange.Type.ADDED) {
                                YearGroup yearGroup = document.getDocument().toObject(YearGroup.class);
                                yearGroup.setID(document.getDocument().getId());
                                yearGroupsListEducator.add(yearGroup);

                            }
                        }
                        userViewModel.setYearGroupsEducator(yearGroupsListEducator);
                    }
                });
    }

    public Task<ArrayList<String>> getEducatorYearGroupIds(String email) {
        TaskCompletionSource<ArrayList<String>> taskCompletionSource = new TaskCompletionSource<>();
        ArrayList<String> result = new ArrayList<>();
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

    public void updateYearGroup(String yearGroupId, String yearGroupName) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .update("name", yearGroupName)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });

        }
    }

    public void updateClassRoom(String classRoomId, String className) {
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupId = userViewModel.getYearGroupID().getValue();
        if (schoolID != null && yearGroupId != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .update("name", className)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });

        }
    }

    public void updateStudent(String studentId, String studentName, String studentEmail) {
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupId = userViewModel.getYearGroupID().getValue();
        String classRoomId = userViewModel.getClassRoomID().getValue();
        if (schoolID != null && yearGroupId != null && classRoomId != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("students")
                    .document(studentId)
                    .update("name", studentName, "email", studentEmail)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });

        }
    }

    public void updateEducator(String educatorId, String educatorName, String educatorEmail) {
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupId = userViewModel.getYearGroupID().getValue();
        String classRoomId = userViewModel.getClassRoomID().getValue();
        if (schoolID != null && yearGroupId != null && classRoomId != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("educators")
                    .document(educatorId)
                    .update("name", educatorName, "email", educatorEmail)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });

        }
    }
    public void updatePublicList(String listId, String listName, ArrayList<String> words) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("lists")
                    .document(listId)
                    .update("name", listName, "words", words)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });

        }
    }


    public void updateList(String listId, String listName, ArrayList<String> words) {
        String schoolID = userViewModel.getSchoolID().getValue();
        String yearGroupId = userViewModel.getYearGroupID().getValue();
        String classRoomId = userViewModel.getClassRoomID().getValue();
        if (schoolID != null && yearGroupId != null && classRoomId != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("lists")
                    .document(listId)
                    .update("name", listName, "words", words)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error updating document", e);
                        }
                    });

        }
    }

    public void deleteYearGroup(String yearGroupId) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");

                        // Refetch the data to update LiveData
                        String role = userViewModel.getRole().getValue();
                        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                        fetchYearGroups(schoolID, role, email);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error deleting document", e);
                    });
        }
    }

    public void deleteClassRoom(String yearGroupId, String classRoomId) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");

                        // Refetch the data to update LiveData
                        String role = userViewModel.getRole().getValue();
                        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                        fetchClassRooms(yearGroupId, role, email);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error deleting document", e);
                    });
        }
    }

    public void deleteStudent(String yearGroupId, String classRoomId, String studentId) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("students")
                    .document(studentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");

                        // Refetch the data to update LiveData
                        String role = userViewModel.getRole().getValue();
                        fetchStudents(yearGroupId, classRoomId, role);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error deleting document", e);
                    });
        }
    }

    public void deleteEducator(String yearGroupId, String classRoomId, String educatorId) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("educators")
                    .document(educatorId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");

                        // Refetch the data to update LiveData
                        String role = userViewModel.getRole().getValue();
                        fetchEducators(yearGroupId, classRoomId, role);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error deleting document", e);
                    });
        }
    }
    public void deleteList(String listId) {
       FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("lists")
                    .document(listId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                        fetchLists();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error deleting document", e);
                    });
        }
    }
    public void deleteList(String yearGroupId, String classRoomId, String listId) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("lists")
                    .document(listId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");

                        // Refetch the data to update LiveData
                        String role = userViewModel.getRole().getValue();
                        fetchLists(yearGroupId, classRoomId, role);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error deleting document", e);
                    });
        }
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
        classRoomArrayListAdmin.clear();
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupId)
                .collection("classes")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            // Handle Firestore errors
                            Log.e("Firestore error", error.getMessage());
                            return;
                        }

                        // Process document changes
                        for (DocumentChange document : value.getDocumentChanges()) {
                            if (document.getType() == DocumentChange.Type.ADDED) {
                                ClassRoom classRoom = document.getDocument().toObject(ClassRoom.class);
                                classRoom.setID(document.getDocument().getId());
                                classRoomArrayListAdmin.add(classRoom);

                            }
                        }
                        userViewModel.setClasses(classRoomArrayListAdmin);
                    }
                });
    }

    private void fetchEducatorClassRooms(String schoolID, String yearGroupId, String email) {
        classRoomArrayListEducator.clear();
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
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                                    if (error != null) {
                                        // Handle Firestore errors
                                        Log.e("Firestore error", error.getMessage());
                                        return;
                                    }

                                    // Check if the QuerySnapshot is null or empty
                                    if (value == null || value.isEmpty()) {
                                        Log.d(TAG, "No documents found in the query.");
                                        return;
                                    }

                                    // Process document changes
                                    for (DocumentChange document : value.getDocumentChanges()) {
                                        if (document.getType() == DocumentChange.Type.ADDED) {
                                            ClassRoom classRoom = document.getDocument().toObject(ClassRoom.class);
                                            classRoom.setID(document.getDocument().getId());
                                            classRoomArrayListEducator.add(classRoom);

                                        }
                                    }
                                    userViewModel.setClassesEducator(classRoomArrayListEducator);
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


    public void fetchStudents(String yearGroupId, String classRoomId, String role) {
        studentArrayList.clear();
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID == null) {
            Log.e(TAG, "School ID is null");
            return;
        }
        if (role.equals("admin") || role.equals("educator")) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("students").orderBy("name", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.e("Firestore error", Objects.requireNonNull(error.getMessage()));
                                return;
                            }
                            assert value != null;
                            for (DocumentChange document : value.getDocumentChanges()) {
                                if (document.getType() == DocumentChange.Type.ADDED) {
                                    Student student = document.getDocument().toObject(Student.class);
                                    student.setID(document.getDocument().getId());
                                    studentArrayList.add(student);
                                }
                            }
                            userViewModel.setStudents(studentArrayList);
                        }
                    });


        }
    }


    public void fetchEducators(String yearGroupId, String classRoomId, String role) {
        educatorArrayList.clear();
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID == null) {
            Log.e(TAG, "School ID is null");
            return;
        }
        if (role.equals("admin")) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("educators").orderBy("name", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.e("Firestore error", Objects.requireNonNull(error.getMessage()));
                                return;
                            }
                            assert value != null;
                            for (DocumentChange document : value.getDocumentChanges()) {
                                if (document.getType() == DocumentChange.Type.ADDED) {
                                    Educator educator = document.getDocument().toObject(Educator.class);
                                    educator.setID(document.getDocument().getId());
                                    educatorArrayList.add(educator);
                                }
                            }
                            userViewModel.setEducators(educatorArrayList);
                        }
                    });


        }
    }
    public void fetchLists() {
        listArrayListParent.clear();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!=null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("lists").orderBy("name", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.e("Firestore error", Objects.requireNonNull(error.getMessage()));
                                return;
                            }
                            assert value != null;
                            for (DocumentChange document : value.getDocumentChanges()) {
                                if (document.getType() == DocumentChange.Type.ADDED) {
                                    WordList wordList = document.getDocument().toObject(WordList.class);
                                    wordList.setId(document.getDocument().getId());
                                    listArrayListParent.add(wordList);
                                }
                            }
                            userViewModel.setLists(listArrayListParent);
                        }
                    });
        }

    }
    public void fetchLists(String yearGroupId, String classRoomId, String role) {
        listArrayList.clear();
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID == null) {
            Log.e(TAG, "School ID is null");
            return;
        }
        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupId)
                .collection("classes")
                .document(classRoomId)
                .collection("lists").orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("Firestore error", Objects.requireNonNull(error.getMessage()));
                            return;
                        }
                        assert value != null;
                        for (DocumentChange document : value.getDocumentChanges()) {
                            if (document.getType() == DocumentChange.Type.ADDED) {
                                WordList wordList = document.getDocument().toObject(WordList.class);
                                wordList.setId(document.getDocument().getId());
                                listArrayList.add(wordList);
                            }
                        }
                        userViewModel.setLists(listArrayList);
                    }
                });


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


    public void saveNewEducator(String yearGroupId, String classRoomId, String name, String email, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID == null || yearGroupId == null || classRoomId == null) {
            onFailure.onFailure(new Exception("School ID or Year Group ID is null"));
            return;
        }

        String role = "educator";
        Map<String, Object> educatorDetails = new HashMap<>();
        educatorDetails.put("name", name);
        educatorDetails.put("email", email);
        educatorDetails.put("role", role);
        educatorDetails.put("schoolId", schoolID);
        educatorDetails.put("yearGroupId", yearGroupId);
        educatorDetails.put("classRoomId", classRoomId);

        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupId)
                .collection("classes")
                .document(classRoomId)
                .collection("educators")
                .add(educatorDetails)
                .addOnSuccessListener(documentReference -> {
                    updateUserRecord(email, name, role, schoolID, onSuccess, onFailure);
                })
                .addOnFailureListener(onFailure);

    }

    public void saveNewYearGroup(String yearGroupName) {
        String schoolID = userViewModel.getSchoolID().getValue();

        if (schoolID != null) {
            Map<String, Object> yearGroupDetails = new HashMap<>();
            yearGroupDetails.put("name", yearGroupName);

            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .add(yearGroupDetails)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
        }
    }


    public void saveNewClass(String yearGroupId, String className) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID != null) {
            Map<String, Object> classDetails = new HashMap<>();
            classDetails.put("name", className);

            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .add(classDetails)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
        }
    }

    public void saveNewStudent(String yearGroupId, String classRoomId, String name, String email, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String schoolID = userViewModel.getSchoolID().getValue();
        if (schoolID == null || yearGroupId == null || classRoomId == null) {
            onFailure.onFailure(new Exception("School ID or Year Group ID is null"));
            return;
        }

        String role = "student";
        Map<String, Object> educatorDetails = new HashMap<>();
        educatorDetails.put("name", name);
        educatorDetails.put("email", email);
        educatorDetails.put("role", role);
        educatorDetails.put("schoolId", schoolID);
        educatorDetails.put("yearGroupId", yearGroupId);
        educatorDetails.put("classRoomId", classRoomId);

        db.collection("schools")
                .document(schoolID)
                .collection("yearGroups")
                .document(yearGroupId)
                .collection("classes")
                .document(classRoomId)
                .collection("students")
                .add(educatorDetails)
                .addOnSuccessListener(documentReference -> {
                    updateUserRecord(email, name, role, schoolID, onSuccess, onFailure);
                })
                .addOnFailureListener(onFailure);

    }

    public void saveNewList(String yearGroupId, String classRoomId, String name, List<String> wordsList, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        String schoolID = userViewModel.getSchoolID().getValue();
        Map<String, Object> listDetails = new HashMap<>();
        listDetails.put("name", name);
        listDetails.put("schoolId", schoolID);
        listDetails.put("yearGroupId", yearGroupId);
        listDetails.put("classId", classRoomId);
        listDetails.put("words", wordsList);
        if (schoolID != null && yearGroupId != null && classRoomId != null) {
            db.collection("schools")
                    .document(schoolID)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classRoomId)
                    .collection("lists")
                    .add(listDetails)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            onSuccess.onSuccess(null);
                        }
                    })
                    .addOnFailureListener(onFailure);
        }
    }

    public void saveNewList(String name, List<String> wordsList, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> listDetails = new HashMap<>();
        listDetails.put("name", name);
        listDetails.put("words", wordsList);
        assert currentUser != null;
        db.collection("users")
                .document(currentUser.getUid())
                .collection("lists")
                .add(listDetails)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        onSuccess.onSuccess(null);
                    }
                })
                .addOnFailureListener(onFailure);
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

    public void fetchChildProfiles() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .collection("childProfiles")
                    .orderBy("name", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (error != null) {
                                Log.e("Firestore error", Objects.requireNonNull(error.getMessage()));
                                return;
                            }
                            assert value != null;
                            for (DocumentChange document : value.getDocumentChanges()) {
                                if (document.getType() == DocumentChange.Type.ADDED) {
                                    ChildProfile childProfile = document.getDocument().toObject(ChildProfile.class);
                                    childProfile.setID(document.getDocument().getId());
                                    childProfilesList.add(childProfile);
                                }
                            }
                            userViewModel.setChildProfiles(childProfilesList);

                        }
                    });
        }
    }
    public void fetchStatistics(String listId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    String email = document.getString("email");
                                    db.collectionGroup("students")
                                            .whereEqualTo("email", email)
                                            .limit(1)
                                            .get()
                                            .addOnCompleteListener(studentTask -> {
                                                if (studentTask.isSuccessful()) {
                                                    QuerySnapshot studentSnapshot = studentTask.getResult();
                                                    if (studentSnapshot != null && !studentSnapshot.isEmpty()) {
                                                        DocumentSnapshot doc = studentSnapshot.getDocuments().get(0);
                                                        String studentId = doc.getId();
                                                        String yearGroupId = doc.getString("yearGroupId");
                                                        String classId = doc.getString("classRoomId");
                                                        String schoolId = doc.getString("schoolId");

                                                        if (schoolId != null && yearGroupId != null && classId != null) {
                                                            db.collection("schools")
                                                                    .document(schoolId)
                                                                    .collection("yearGroups")
                                                                    .document(yearGroupId)
                                                                    .collection("classes")
                                                                    .document(classId)
                                                                    .collection("students")
                                                                    .document(studentId)
                                                                    .collection("statistics")
                                                                    .whereEqualTo("listId", listId)
                                                                    .limit(1)
                                                                    .get()
                                                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                                                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                                                                String statisticsId = document.getId();
                                                                                // Call with full path info
                                                                                fetchAttempts(listId, statisticsId, schoolId, yearGroupId, classId, studentId);
                                                                            } else {
                                                                                Log.d(TAG, "No statistics document found");
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    });
        }
    }

    public void fetchAttempts(String listId, String statisticsId,
                              String schoolId, String yearGroupId, String classId, String studentId) {
        if (statisticsId != null) {
            List<String> wordList = new ArrayList<>();

            db.collection("schools")
                    .document(schoolId)
                    .collection("yearGroups")
                    .document(yearGroupId)
                    .collection("classes")
                    .document(classId)
                    .collection("students")
                    .document(studentId)
                    .collection("statistics")
                    .document(statisticsId)
                    .collection("attempts")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Query successful, document count: " + task.getResult().size());

                                boolean add = false;

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, "Processing document: " + document.getId());

                                    List<Map<String, String>> wordAttempts = (List<Map<String, String>>) document.get("wordAttempts");

                                    if (wordAttempts != null) {
                                        for (Map<String, String> attempt : wordAttempts) {
                                            String word = attempt.get("word");
                                            String attemptsStr = attempt.get("attempts");

                                            if (attemptsStr != null) {
                                                try {
                                                    int attempts = Integer.parseInt(attemptsStr);
                                                    if (attempts == 6) {
                                                        Log.d(TAG, "Adding word to list: " + word);
                                                        wordList.add(word);
                                                        add = true;
                                                    }
                                                } catch (NumberFormatException e) {
                                                    Log.e(TAG, "Failed to parse attempts value: " + attemptsStr, e);
                                                }
                                            }
                                        }
                                    } else {
                                        Log.w(TAG, "wordAttempts is null for document: " + document.getId());
                                    }
                                }

                                if (add) {
                                    incorrectWordsByList.put(listId, wordList);
                                    userViewModel.setIncorrectWords(incorrectWordsByList);
                                    Log.d(TAG, "Words added to incorrectWordsByList");
                                } else {
                                    Log.d(TAG, "No words to add, add flag is false");
                                }
                            } else {
                                Log.e(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
        } else {
            Log.w(TAG, "statisticsId is null, cannot fetch attempts");
        }
    }




}




