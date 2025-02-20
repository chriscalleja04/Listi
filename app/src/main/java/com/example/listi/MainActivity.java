package com.example.listi;

import static android.app.PendingIntent.getActivity;
import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.listi.databinding.ActivityMainBinding;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private UserViewModel userViewModel;
    private FirebaseAuth firebaseAuth;

    private FirebaseAuth.AuthStateListener authStateListener;
    private OAuthProvider.Builder provider;
    private Button button,logoutButton;
    private TextView navUsername, navEmail;


    private ImageView profile;
    private FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_accessibility)
                .setOpenableLayout(drawer)
                .build();
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        View headerView = navigationView.getHeaderView(0);

        // THEN initialize the views from the header
        navUsername = headerView.findViewById(R.id.username);
        navEmail = headerView.findViewById(R.id.navEmail);
        //profile = headerView.findViewById(R.id.profilePicture);
        //logoutButton = headerView.findViewById(R.id.logout);


        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    fetchRoles(user.getUid());

                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    updateNavigationMenu("public");

                }
            }
        };



        db = FirebaseFirestore.getInstance();

        provider = OAuthProvider.newBuilder("microsoft.com")
                .addCustomParameter("ui_locales", "mt-MT")
                .addCustomParameter("prompt", "select_account")
                .addCustomParameter("tenant", "common")
                .addCustomParameter("mkt", "mt-MT")
                .addCustomParameter("locale", "mt-MT");


        checkPendingResult();

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userViewModel.setUser(currentUser);




    }
    private void fetchRoles(String id){
        db.collection("users")
                .document(id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            DocumentSnapshot document = task.getResult();
                            if(document.exists()){
                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                String role = document.getString("role");
                                if(role!=null) {
                                    userViewModel.setRole(role);
                                    updateNavigationMenu(role);

                                }

                            }else{
                                Log.d(TAG, "No such document");
                            }
                        }else{
                            Log.d(TAG, "get failed with ", task.getException());

                        }
                    }
                });

    }

    public void updateNavigationMenu(String role){
        Menu menu = binding.navView.getMenu();
        if(role!=null) {
            if (role.equals("admin")) {
                menu.findItem(R.id.nav_staff_management).setVisible(true);
                menu.findItem(R.id.nav_student_management).setVisible(true);
                mAppBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.nav_home, R.id.nav_accessibility, R.id.nav_staff_management, R.id.nav_student_management)
                        .setOpenableLayout(binding.drawerLayout)
                        .build();
            } else if (role.equals("educator")) {
                menu.findItem(R.id.nav_staff_management).setVisible(false);
                menu.findItem(R.id.nav_student_management).setVisible(true);  // Show student management
                mAppBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.nav_home, R.id.nav_accessibility, R.id.nav_student_management)
                        .setOpenableLayout(binding.drawerLayout)
                        .build();
            } else{
                // Public user only sees home and accessibility
                menu.findItem(R.id.nav_staff_management).setVisible(false);
                menu.findItem(R.id.nav_student_management).setVisible(false);
                mAppBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.nav_home, R.id.nav_accessibility)
                        .setOpenableLayout(binding.drawerLayout)
                        .build();
            }
            NavigationUI.setupWithNavController(binding.navView, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));

        }
    }
    private void checkPendingResult() {

        Task<AuthResult> pendingResultTask = firebaseAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            // There's something already here! Finish the sign-in for your user.
            pendingResultTask
                    .addOnSuccessListener(
                            new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    // User is signed in.
                                    // IdP data available in
                                    //authResult.getAdditionalUserInfo().getProfile().
                                    // The OAuth access token can also be retrieved:
                                    // ((OAuthCredential)authResult.getCredential()).getAccessToken().
                                    // The OAuth secret can be retrieved by calling:
                                    // ((OAuthCredential)authResult.getCredential()).getSecret().
                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    // Handle failure.
                                }
                            });
        } else {
            // There's no pending result so you need to start the sign-in flow.
            // See below.
        }
    }

    public void startSignInFlow() {
        firebaseAuth
                .startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(
                        new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                // User is signed in.
                                // IdP data available in
                                // authResult.getAdditionalUserInfo().getProfile().
                                // The OAuth access token can also be retrieved:
                                // ((OAuthCredential)authResult.getCredential()).getAccessToken().
                                // The OAuth secret can be retrieved by calling:
                                // ((OAuthCredential)authResult.getCredential()).getSecret().
                                FirebaseUser user = authResult.getUser();
                                if(user!=null){
                                    userViewModel.setUser(user);
                                    updateUserData(user);
                                    DocumentReference docRef = db.collection("users").document(user.getUid());
                                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if(task.isSuccessful()){
                                                DocumentSnapshot document = task.getResult();
                                                if(document.exists()){
                                                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                                } else{
                                                    addUserToFirestore(user);
                                                    Log.d(TAG, "No such document");
                                                }
                                            }else{
                                                Log.d(TAG, "get failed with ", task.getException());
                                            }
                                        }
                                    });





                                }else{

                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Handle failure.
                            }
                        });

    }


    private void addUserToFirestore(FirebaseUser user) {
        if (user != null) {
            String uid = user.getUid();
            String email = user.getEmail();
            String name = user.getDisplayName();

            // Check if the user is an educator
            checkIfEducator(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String schoolID = task.getResult();
                    String role = null;
                    if(schoolID!=null){
                        role = "educator";
                    }else{
                        role = "public";
                    }
                    // Create user data
                    Map<String, Object> userDetails = new HashMap<>();
                    userDetails.put("email", email);
                    userDetails.put("name", name);
                    userDetails.put("role", role);
                    userDetails.put("schoolId",schoolID);
                    // Add user to Firestore
                    db.collection("users")
                            .document(uid)
                            .set(userDetails)
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "User added to Firestore");
                            })
                            .addOnFailureListener(e -> {
                                Log.w(TAG, "Error adding user to Firestore", e);
                            });
                    updateNavigationMenu(role);


                } else {
                    Log.w(TAG, "Error checking educator status", task.getException());
                }

            });


        }
    }


    public Task<String> checkIfEducator(String email) {
        // Create a TaskCompletionSource to properly handle the async result
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

        db.collectionGroup("educators")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean isEducator = !querySnapshot.isEmpty();
                    String schoolID = null;
                    Log.d(TAG, "Educator check completed. Found: " + isEducator + " for email: " + email);
                    if (isEducator) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        schoolID = doc.getString("schoolId");
                        Log.d(TAG, "Found educator in path: " + doc.getReference().getPath());
                    }
                    taskCompletionSource.setResult(schoolID);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking educator status for email: " + email, e);
                    taskCompletionSource.setException(e);
                });

        return taskCompletionSource.getTask();
    }

    public void fetchSchoolID(String userID){
        DocumentReference docRef = db.collection("users").document(userID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        String id = document.getString("schoolId");
                        userViewModel.setSchoolID(id);

                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else{
                        Log.d(TAG, "No such document");
                    }
                }else{
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }

    public void fetchSchoolName(String schoolID){
        DocumentReference docRef = db.collection("schools").document(schoolID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        String name = document.getString("name");
                        userViewModel.setSchoolName(name);
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else{
                        Log.d(TAG, "No such document");
                    }
                }else{
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }
    public void fetchYearGroups(String schoolID) {
       db.collection("schools")
               .document(schoolID)
               .collection("yearGroups")
               .get()
               .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                   @Override
                   public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            List<String> yearGroupNames = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()){
                                String name = document.getString("name");
                                if(name != null){
                                    yearGroupNames.add(name);
                                }
                                Log.d(TAG, document.getId() + " => " + document.getData());
                            }
                            userViewModel.setYearGroups(yearGroupNames);
                        }else{
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                   }
               });


    }

    public void signOut(){
        FirebaseAuth.getInstance().signOut();
        clearData();

    }

    private void clearData(){
        userViewModel.setUser(null);
        navUsername.setText("Guest");
        navEmail.setText("Guest123");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if a user is signed in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userViewModel.setUser(currentUser);

        if (currentUser != null) {
            // User is logged in, update UI with their information
            updateUserData(currentUser);
        }

        if (authStateListener != null) {
            firebaseAuth.addAuthStateListener(authStateListener);
        }
    }

    private void updateUserData(FirebaseUser user) {



                navUsername.setText(user.getDisplayName());

                navEmail.setText(user.getEmail());

            // Fetch the data from Firestore
            fetchSchoolID(user.getUid());

            userViewModel.getSchoolID().observe(this, schoolId -> {
                if (schoolId != null) {
                    fetchSchoolName(schoolId);
                    fetchYearGroups(schoolId);
                } else {
                    Log.d(TAG, "School ID is null, can't fetch school data");
                }
            });

    }
    @Override
    public void onStop(){
        super.onStop();
        if(authStateListener != null){
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}