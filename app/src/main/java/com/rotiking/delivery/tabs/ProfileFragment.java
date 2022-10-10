package com.rotiking.delivery.tabs;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rotiking.delivery.MyPhotoActivity;
import com.rotiking.delivery.R;
import com.rotiking.delivery.common.auth.Auth;
import com.rotiking.delivery.common.auth.AuthPreferences;
import com.rotiking.delivery.utils.Promise;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class ProfileFragment extends Fragment {
    private View view;
    private ImageView myPhoto;
    private TextView myNameTxt, emailTxt, usernameTxt;
    private AppCompatButton logoutBtn, changePhotoBtn;

    private String photo = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        myPhoto = view.findViewById(R.id.photo);
        myNameTxt = view.findViewById(R.id.my_name);
        emailTxt = view.findViewById(R.id.email);
        changePhotoBtn = view.findViewById(R.id.edit_photo);
        logoutBtn = view.findViewById(R.id.logout);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        myNameTxt.setText(Auth.getAuthUserName());
        emailTxt.setText(Auth.getAuthUserEmail());

        FirebaseFirestore.getInstance().collection("user").document(Objects.requireNonNull(Auth.getAuthUserUid())).collection("data").document("profile").get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.get("photo", String.class).equals("")) {
                photo = documentSnapshot.get("photo", String.class);
                Glide.with(view.getContext()).load(photo).into(myPhoto);
            }
        });

        changePhotoBtn.setOnClickListener(view1 -> {
            Intent intent = new Intent(view.getContext(), MyPhotoActivity.class);
            intent.putExtra("PHOTO", photo);
            startActivity(intent);
        });

        logoutBtn.setOnClickListener(view1 -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
            alert.setTitle("Logout");
            alert.setMessage("Are you sure, you want to logout.");
            alert.setCancelable(true);
            alert.setPositiveButton("Yes", (dialogInterface, i) -> {
                AuthPreferences preferences = new AuthPreferences(view.getContext());
                preferences.clear();
                FirebaseAuth.getInstance().signOut();
            });
            alert.setNegativeButton("No", (dialogInterface, i) -> dialogInterface.dismiss());
            alert.show();
        });
    }
}