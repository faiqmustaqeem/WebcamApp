package com.serenegiant.usbcameratest;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SigninActivity extends AppCompatActivity {

    EditText et_email, et_passowrd;
    Button btn_login;

    FirebaseDatabase database;
    DatabaseReference ref;
    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        database = FirebaseDatabase.getInstance();
        ref = database.getReference();

        et_email = (EditText) findViewById(R.id.input_email);
        et_passowrd = (EditText) findViewById(R.id.input_password);
        btn_login = (Button) findViewById(R.id.btn_login);
        dialog = new ProgressDialog(this);
        dialog.setTitle("Checking");
        dialog.setMessage("Wait...");


        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
                checkUSerExists();
            }
        });

    }

    private void checkUSerExists() {
        ref.child("webcam_app").child("users").orderByChild("email").equalTo(et_email.getText().toString()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e("snap", dataSnapshot.toString());
                if (dataSnapshot.exists()) {
                    for (final DataSnapshot sc : dataSnapshot.getChildren()) {
                        if (sc.child("password").getValue(String.class).equals(et_passowrd.getText().toString())) {
                            if (sc.child("isLoggedIn").getValue(Boolean.class).equals(false)) {
                                ref.child("webcam_app").child("users").child(sc.getKey()).child("isLoggedIn").setValue(true, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                        dialog.dismiss();
                                        SharedPreferenceHelper.setSharedPreferenceString(SigninActivity.this, "email", et_email.getText().toString());
                                        SharedPreferenceHelper.setSharedPreferenceBoolean(SigninActivity.this, "isLoggedIn", true);
                                        SharedPreferenceHelper.setSharedPreferenceString(SigninActivity.this, "key", sc.getKey());

                                        Intent intent = new Intent(SigninActivity.this, MainActivity.class);
                                        startActivity(intent);
                                    }
                                });

                            } else {
                                dialog.dismiss();
                                Toast.makeText(SigninActivity.this, "Already logged in from another mobile", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            dialog.dismiss();
                            Toast.makeText(SigninActivity.this, "Invalid Password", Toast.LENGTH_SHORT).show();
                        }
                    }

                } else {
                    dialog.dismiss();
                    Toast.makeText(SigninActivity.this, "Invalid Email", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
