package com.example.temp;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private boolean isMainGraphLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //
        // ✅ Enable full edge-to-edge UI
        //
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setVisibility(BottomNavigationView.GONE);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        if (navController != null) {
            navController.setGraph(R.navigation.entry_nav_graph);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (!isMainGraphLoaded) return false;
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        //
        // ✅ Apply proper WindowInsets to handle padding around status/nav bars
        //
        applyInsets();
    }

    /**
     * Switch to main navigation graph after splash/entry screen.
     */
    public void switchToMainGraph() {
        if (navController == null || isMainGraphLoaded) return;

        navController.setGraph(R.navigation.main_nav_graph);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        bottomNavigationView.setVisibility(BottomNavigationView.VISIBLE);

        isMainGraphLoaded = true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }


    /**
     * ✅ Handles edge-to-edge UI padding correctly for fragments inside navHostFragment.
     */
    private void applyInsets() {
        View navHost = findViewById(R.id.nav_host_fragment);

        ViewCompat.setOnApplyWindowInsetsListener(navHost, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            // Apply safe padding to fragment content root
            v.setPadding(
                    v.getPaddingLeft(),
                    top,
                    v.getPaddingRight(),
                    bottom
            );

            return insets;
        });
    }
}
