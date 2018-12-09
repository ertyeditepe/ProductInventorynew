package com.example.etoo.productinventory.authentication;

import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.etoo.productinventory.InventoryActivity;
import com.example.etoo.productinventory.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    public TextView signUp, loginButton, forgotPassword;
    public EditText userMailEditText, userPasswordEditText;
    LinearLayout loginLayout;
    private ProgressBar loginProgressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onStart() {
        super.onStart();

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.login_page);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // User is signed in
            Intent i = new Intent(LoginActivity.this, InventoryActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }

        loginLayout = findViewById(R.id.loginLayout);
        loginProgressBar = findViewById(R.id.loginProgress);
        signUp = findViewById(R.id.signUpText);
        forgotPassword = findViewById(R.id.forgotPass);
        userMailEditText = findViewById(R.id.editUserMail);
        userPasswordEditText = findViewById(R.id.editUserPassword);
        loginButton = findViewById(R.id.loginTextView);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LoginUser();
            }
        });
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    public void LoginUser() {
        String Email = userMailEditText.getText().toString();
        String Password = userPasswordEditText.getText().toString();
        loginProgressBar.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.INVISIBLE);
        mAuth.signInWithEmailAndPassword(Email, Password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(getApplicationContext(),
                                    InventoryActivity.class));
                            finish();
                            loginProgressBar.setVisibility(View.INVISIBLE);
                        } else {
                            Toast.makeText(LoginActivity.this, "Couldn't login, please check your connection",
                                    Toast.LENGTH_SHORT).show();
                            loginProgressBar.setVisibility(View.INVISIBLE);
                            loginLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

}

