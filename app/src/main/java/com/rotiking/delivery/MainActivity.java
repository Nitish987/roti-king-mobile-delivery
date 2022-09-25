package com.rotiking.delivery;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rotiking.delivery.common.auth.Auth;
import com.rotiking.delivery.tabs.OrdersFragment;
import com.rotiking.delivery.tabs.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Auth.isUserAuthenticated(this)) {
            toLoginActivity();
        }

        tabs = findViewById(R.id.tabs);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onStart() {
        super.onStart();
        Auth.setAuthStateListener(firebaseAuth -> {
            if (!Auth.isUserAuthenticated(this)) {
                toLoginActivity();
            }
        });

        tabs.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.orders:
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new OrdersFragment()).commit();
                    break;
                case R.id.profile:
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).commit();
                    break;
            }
            return true;
        });
    }

    private void toLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}