    package com.example.listi;

    import static android.app.PendingIntent.getActivity;
    import static android.content.ContentValues.TAG;

    import android.content.Context;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.MenuItem;
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
    import androidx.core.content.ContextCompat;
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
    import com.google.firebase.firestore.FieldPath;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.QueryDocumentSnapshot;
    import com.google.firebase.firestore.QuerySnapshot;

    import org.w3c.dom.Text;

    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Objects;

    public class MainActivity extends AppCompatActivity {

        private AppBarConfiguration mAppBarConfiguration;
        private ActivityMainBinding binding;

        private UserViewModel userViewModel;

        private YearClassRepository yearClassRepository;
        private AuthViewModel authViewModel;
        private FirebaseAuth firebaseAuth;

        private FirebaseAuth.AuthStateListener authStateListener;
        private OAuthProvider.Builder provider;
        private Button button, logoutButton;
        private TextView navUsername, navEmail;
        private AuthRepository authRepository;

        private ImageView profile;
        private FirebaseFirestore db;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ThemeManager.applyTheme(this);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            setSupportActionBar(binding.appBarMain.toolbar);


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
            configureNavigationView();

            View headerView = navigationView.getHeaderView(0);

            // THEN initialize the views from the header
            navUsername = headerView.findViewById(R.id.username);
            navEmail = headerView.findViewById(R.id.navEmail);
            //profile = headerView.findViewById(R.id.profilePicture);
            //logoutButton = headerView.findViewById(R.id.logout);

            userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
            authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

            SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String childId = prefs.getString("child_id", null);
            String childName = prefs.getString("child_name", null);
            if (childId != null && childName != null) {
                userViewModel.setChildID(childId);
                userViewModel.setChildName(childName);
            }

            db = FirebaseFirestore.getInstance();
            authRepository = new AuthRepository();
            yearClassRepository = new YearClassRepository(userViewModel);


            authStateListener = firebaseAuth -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    fetchRoles(user.getUid());
                    userViewModel.setUser(user);
                    authViewModel.setUser(user);
                    updateUserData(user);
                    Log.d(TAG, "onAuthStateChanged:signed_in: " + user.getUid());
                } else {
                    userViewModel.setUser(null);
                    authViewModel.setUser(null);
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    updateNavigationMenu("public");
                    navUsername.setText(getResources().getText(R.string.nav_header_title));
                    navEmail.setText(getResources().getText(R.string.nav_header_subtitle));
                }
            };

            authRepository.addAuthStateListener(authStateListener);


            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        // User is signed in
                        fetchRoles(user.getUid());
                        Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    } else {
                        // User is signed out
                        Log.d(TAG, "onAuthStateChanged:signed_out");

                        // Update the navigation menu to the public state
                        updateNavigationMenu("public");
                        navUsername.setText(getResources().getText(R.string.nav_header_title));
                        navEmail.setText(getResources().getText(R.string.nav_header_subtitle));
                    }
                }
            };

            authViewModel.getCurrentUser().observe(this, user -> {
                if(user != null){
                    updateUserData(user);
                }else{
                    navUsername.setText(getResources().getText(R.string.nav_header_title));
                    navEmail.setText(getResources().getText(R.string.nav_header_subtitle));
                    updateNavigationMenu("public");
                }
            });

            authViewModel.getUserRole().observe(this,role ->{
                if(role != null){
                    updateNavigationMenu(role);
                }
            });

            authViewModel.getAuthMessage().observe(this, message -> {
                if(message != null && !message.isEmpty()){
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                }
            });


        }


        public void startSignInFlow(){
            authViewModel.loginWithMicrosoft(this);
        }

        public void signOut(){
            authViewModel.signOut();
            clearData();
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
                                    String schoolId = document.getString("schoolId");
                                    String email = document.getString("email");
                                    if(role!=null) {
                                        userViewModel.setRole(role);
                                        if(schoolId != null) {
                                            userViewModel.setSchoolID(schoolId);
                                            Log.d(TAG, "Fetching school name with ID: " + schoolId);
                                            yearClassRepository.fetchSchoolName(schoolId);
                                            yearClassRepository.fetchYearGroups(schoolId, role, email);
                                        }
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

        public void updateNavigationMenu(String role) {
            Menu menu = binding.navView.getMenu();
            if (role != null) {
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
                } else if (role.equals("public")) {
                    menu.findItem(R.id.nav_staff_management).setVisible(false);
                    menu.findItem(R.id.nav_student_management).setVisible(true);
                    MenuItem menuItem = menu.findItem(R.id.nav_student_management);
                    if (menuItem != null) {
                        menuItem.setTitle("Statistika");
                        menuItem.setVisible(true);
                    }// Show student management
                    binding.navView.setCheckedItem(R.id.nav_home);
                    mAppBarConfiguration = new AppBarConfiguration.Builder(
                            R.id.nav_home, R.id.nav_accessibility, R.id.nav_student_management)
                            .setOpenableLayout(binding.drawerLayout)
                            .build();
                    userViewModel.getChildID().observe(this, id -> {
                        if (id != null) {
                            menu.findItem(R.id.nav_staff_management).setVisible(false);
                            menu.findItem(R.id.nav_student_management).setVisible(false);  // Show student management
                            binding.navView.setCheckedItem(R.id.nav_home);
                            mAppBarConfiguration = new AppBarConfiguration.Builder(
                                    R.id.nav_home, R.id.nav_accessibility)
                                    .setOpenableLayout(binding.drawerLayout)
                                    .build();

                        }
                    });
                            /*
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                db.collection("users")
                                        .document(currentUser.getUid())
                                        .collection("childProfiles")
                                        .limit(1)
                                        .get()
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                QuerySnapshot snapshot = task.getResult();
                                                if (snapshot != null && !snapshot.isEmpty()) {
                                                    menu.findItem(R.id.nav_staff_management).setVisible(false);
                                                    menu.findItem(R.id.nav_student_management).setVisible(true);
                                                    mAppBarConfiguration = new AppBarConfiguration.Builder(
                                                            R.id.nav_home, R.id.nav_accessibility, R.id.nav_student_management)
                                                            .setOpenableLayout(binding.drawerLayout)
                                                            .build();

                                                } else {
                                                    menu.findItem(R.id.nav_staff_management).setVisible(false);
                                                    menu.findItem(R.id.nav_student_management).setVisible(false);
                                                    mAppBarConfiguration = new AppBarConfiguration.Builder(
                                                            R.id.nav_home, R.id.nav_accessibility)
                                                            .setOpenableLayout(binding.drawerLayout)
                                                            .build();
                                                }
                                                NavigationUI.setupWithNavController(binding.navView, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
                                                binding.navView.setCheckedItem(R.id.nav_home); // Explicitly set Home as selected
                                            } else {
                                                // Handle the error
                                                Exception e = task.getException();
                                                if (e != null) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                            }
                        } else {
                            menu.findItem(R.id.nav_staff_management).setVisible(false);
                            menu.findItem(R.id.nav_student_management).setVisible(false);
                            mAppBarConfiguration = new AppBarConfiguration.Builder(
                                    R.id.nav_home, R.id.nav_accessibility)
                                    .setOpenableLayout(binding.drawerLayout)
                                    .build();
                            NavigationUI.setupWithNavController(binding.navView, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
                            binding.navView.setCheckedItem(R.id.nav_home); // Explicitly set Home as selected
                        }
                    });*/
                } else {
                           // Public user only sees home and accessibility
                           menu.findItem(R.id.nav_staff_management).setVisible(false);
                           menu.findItem(R.id.nav_student_management).setVisible(false);
                           mAppBarConfiguration = new AppBarConfiguration.Builder(
                                   R.id.nav_home, R.id.nav_accessibility)
                                   .setOpenableLayout(binding.drawerLayout)
                                   .build();
                           NavigationUI.setupWithNavController(binding.navView, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));
                           binding.navView.setCheckedItem(R.id.nav_home); // Explicitly set Home as selected
                       }
                       // Move this line inside the role checks to ensure it's called after the menu is configured
                       // NavigationUI.setupWithNavController(binding.navView, Navigation.findNavController(this, R.id.nav_host_fragment_content_main));

                }
        }



        public void updateUserData(FirebaseUser user) {
            navUsername.setText(user.getDisplayName());
            navEmail.setText(user.getEmail());

            String childId = userViewModel.getChildID().getValue();
            String childName = userViewModel.getChildName().getValue();

            if(childId != null && childName != null){
                navUsername.setText(childName);

            }
            userViewModel.getChildID().observe(this, id -> {
                if(id!=null){
                    String name = userViewModel.getChildName().getValue();
                    if(name != null){
                        navUsername.setText(name);
                    }

                }else{
                    FirebaseUser currentUser = authRepository.getCurrentUser();
                    if(currentUser != null){
                        navUsername.setText(currentUser.getDisplayName());
                    }
                }
            });

                authViewModel.getSchoolID().observe(this, schoolId ->{
                if(schoolId != null){
                    userViewModel.setSchoolID(schoolId);
                    yearClassRepository.fetchSchoolName(schoolId);
                    authViewModel.getUserRole().observe(this, role ->{
                        if(role != null && user.getEmail() != null){
                            yearClassRepository.fetchYearGroups(schoolId, role, user.getEmail());
                        }
                    });
                }else{
                    Log.d(TAG, "School ID is null, cant fetch school data");
                }
            });
        }

        private void clearData(){
            userViewModel.setUser(null);
            updateNavigationMenu("public");
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

        private void configureNavigationView() {
            NavigationView navigationView = binding.navView;
            FontManager fontManager = new FontManager(this);
            ColourManager colourManager = new ColourManager(this);
            String fontType = fontManager.getFontType();
            String colourType = colourManager.getColourType();

            if (fontType.equals(FontManager.FONT_OPEN_DYSLEXIC)) {
                binding.navView.setItemTextAppearance(R.style.NavigationDrawerText_OpenDyslexic);
            } else {
                binding.navView.setItemTextAppearance(R.style.NavigationDrawerText);
            }

            if(fontType.equals(FontManager.FONT_OPEN_DYSLEXIC)){
                if(colourType.equals(ColourManager.COLOUR_2)){
                    navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface_2));
                }else{
                    navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface));
                }
            }else {
                if (colourType.equals(ColourManager.COLOUR_2)) {
                    navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface_2));
                }else{
                    navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface));
                }
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            FirebaseUser currentUser = authRepository.getCurrentUser();
            userViewModel.setUser(currentUser);


            if (currentUser != null) {
                // User is logged in, update UI with their information
                updateUserData(currentUser);
                userViewModel.getRole().observe(this, role ->{
                    updateNavigationMenu(role);

                });
            }
        }



        @Override
        public void onStop(){
            super.onStop();
            if(authStateListener != null){
                authRepository.removeAuthStateListener(authStateListener);
            }

        }
    }

