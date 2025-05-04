    package com.example.listi;

    import static android.app.PendingIntent.getActivity;
    import static android.content.ContentValues.TAG;

    import android.content.Context;
    import android.content.SharedPreferences;
    import android.content.pm.ActivityInfo;
    import android.net.ConnectivityManager;
    import android.net.Network;
    import android.net.NetworkCapabilities;
    import android.os.Build;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.View;
    import android.view.Menu;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.ImageView;
    import android.widget.TextView;

    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.Task;
    import com.google.android.material.snackbar.Snackbar;
    import com.google.android.material.navigation.NavigationView;

    import androidx.annotation.NonNull;
    import androidx.core.content.ContextCompat;
    import androidx.lifecycle.ViewModelProvider;
    import androidx.navigation.NavController;
    import androidx.navigation.Navigation;
    import androidx.navigation.ui.AppBarConfiguration;
    import androidx.navigation.ui.NavigationUI;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.window.core.layout.WindowHeightSizeClass;
    import androidx.window.core.layout.WindowSizeClass;
    import androidx.window.core.layout.WindowWidthSizeClass;
    import androidx.window.layout.WindowMetrics;
    import androidx.window.layout.WindowMetricsCalculator;

    import com.example.listi.databinding.ActivityMainBinding;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.auth.OAuthProvider;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;

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

        private ConnectivityManager connectivityManager;
        private ConnectivityManager.NetworkCallback networkCallback;
        private TextView connectionStatusTextView;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

            if (compactScreen()) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            }
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    runOnUiThread(() -> {
                        userViewModel.setNetworkAvailable(true);
                        Log.d(TAG, "Network Available");
                    });
                }

                @Override
                public void onLost(Network network) {
                    runOnUiThread(() -> {
                        userViewModel.setNetworkAvailable(false);
                        Log.d(TAG, "Network Lost");  // Show message when disconnected
                    });
                }
            };
            ThemeManager.applyTheme(this);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            setSupportActionBar(binding.appBarMain.toolbar);


            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_accessibility)
                    .setOpenableLayout(binding.drawerLayout)
                    .build();
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.


            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.d("Navigation", "Navigated to: " + destination.getLabel() +
                        " with ID: " + destination.getId());
            });
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(binding.navView, navController);
            configureNavigationView();

            View headerView = binding.navView.getHeaderView(0);

            // THEN initialize the views from the header
            navUsername = headerView.findViewById(R.id.username);
            navEmail = headerView.findViewById(R.id.navEmail);
            //profile = headerView.findViewById(R.id.profilePicture);
            //logoutButton = headerView.findViewById(R.id.logout);

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
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                if (role != null) {
                    if (role.equals("admin")) {
                        menu.findItem(R.id.nav_statistics).setVisible(false);
                        menu.findItem(R.id.nav_school_management).setVisible(true);
                        menu.findItem(R.id.nav_lists).setVisible(false);
                    } else if (role.equals("educator")) {
                        menu.findItem(R.id.nav_statistics).setVisible(false);
                        menu.findItem(R.id.nav_school_management).setVisible(true);
                        menu.findItem(R.id.nav_lists).setVisible(false);
                    } else if (role.equals("public")) {
                        menu.findItem(R.id.nav_school_management).setVisible(false);
                        menu.findItem(R.id.nav_statistics).setVisible(true);
                        menu.findItem(R.id.nav_lists).setVisible(true);

                        userViewModel.getChildID().observe(this, id -> {
                            if (id != null) {
                                menu.findItem(R.id.nav_school_management).setVisible(false);
                                menu.findItem(R.id.nav_statistics).setVisible(false);
                            }
                        });
                    } else {
                        // Public user only sees home and accessibility
                        menu.findItem(R.id.nav_school_management).setVisible(false);
                        menu.findItem(R.id.nav_statistics).setVisible(false);
                        menu.findItem(R.id.nav_lists).setVisible(true);
                    }
                }
            } else {
                menu.findItem(R.id.nav_school_management).setVisible(false);
                menu.findItem(R.id.nav_statistics).setVisible(false);
                menu.findItem(R.id.nav_lists).setVisible(false);
            }
            binding.navView.invalidate();
            binding.navView.setCheckedItem(R.id.nav_home);
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

       /* @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }*/
         
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
            } else if (fontType.equals(FontManager.FONT_ANDIKA)) {
                binding.navView.setItemTextAppearance(R.style.NavigationDrawerText_Andika);
            } else { // Default font
                binding.navView.setItemTextAppearance(R.style.NavigationDrawerText);
            }

            if (colourType.equals(ColourManager.COLOUR_2)) {
                navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface_2));
            } else if (colourType.equals(ColourManager.COLOUR_3)) {
                navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface_3));
            } else { // Default color
                navigationView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorSurface));
            }
        }

        /** Determines whether the device has a compact screen. **/
        public boolean compactScreen() {
            WindowMetrics metrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(this);
            int width = metrics.getBounds().width();
            int height = metrics.getBounds().height();
            float density = getResources().getDisplayMetrics().density;
            WindowSizeClass windowSizeClass = WindowSizeClass.compute(width/density, height/density);
            return windowSizeClass.getWindowWidthSizeClass() == WindowWidthSizeClass.COMPACT ||
                    windowSizeClass.getWindowHeightSizeClass() == WindowHeightSizeClass.COMPACT;
        }

        @Override
        public void onStart() {
            super.onStart();
            FirebaseUser currentUser = authRepository.getCurrentUser();
            userViewModel.setUser(currentUser);

            connectivityManager.registerDefaultNetworkCallback(networkCallback);

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
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
            if(authStateListener != null){
                authRepository.removeAuthStateListener(authStateListener);
            }

        }
    }

