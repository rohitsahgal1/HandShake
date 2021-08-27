package com.example.handshake;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserDetailActivity extends AppCompatActivity {

    ImageView dpImageView;
    EditText firstNameEditText,lastNameEditText,emailIdEditText;
    Button doneButton;

    String userMobileNumber;
    String profileImageUri;
    Boolean imageUpdateFlag = false;

    Uri imageUri;
    private static final int PICK_IMAGE = 1;

    FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    DocumentReference documentReference;

    StorageReference storageReference;

    SharedPreferences sharedPreferences = null;
    Boolean nightModeFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        getSupportActionBar().hide();

        sharedPreferences = getSharedPreferences("night",0);
        nightModeFlag = sharedPreferences.getBoolean("night_mode",true);
        if (nightModeFlag) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        dpImageView = (ImageView)findViewById(R.id.dpImageView);
        firstNameEditText = (EditText)findViewById(R.id.firstNameEditText);
        lastNameEditText = (EditText)findViewById(R.id.lastNameEditText);
        emailIdEditText = (EditText)findViewById(R.id.emailIdEditText);
        doneButton = (Button)findViewById(R.id.doneButton);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        userMobileNumber = mAuth.getCurrentUser().getPhoneNumber().toString();

        if (mAuth.getCurrentUser() != null) {
            fetchData();
        }


        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!firstNameEditText.getText().toString().equals("") && !emailIdEditText.getText().toString().equals("")) {
                    uploadData();

                } else {
                    Toast.makeText(UserDetailActivity.this, "Please fill the first Name and email section", Toast.LENGTH_SHORT).show();
                }

            }
        });

        dpImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Pick an image"), PICK_IMAGE);
            }
        });

    }

    private void fetchData() {
        documentReference = firebaseFirestore.collection("Users").document(userMobileNumber);
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                firstNameEditText.setText(documentSnapshot.getString("firstName"));
                lastNameEditText.setText(documentSnapshot.getString("lastName"));
                emailIdEditText.setText(documentSnapshot.getString("email"));
                profileImageUri = documentSnapshot.getString("profileImageUri");
                Glide.with(getApplicationContext()).load(profileImageUri).into(dpImageView);

                firstNameEditText.setEnabled(true);
                lastNameEditText.setEnabled(true);
                emailIdEditText.setEnabled(true);
                doneButton.setEnabled(true);
            }
        });
    }

    private void uploadData() {
        documentReference = firebaseFirestore.collection("Users").document(userMobileNumber);
        Map<String,Object> user = new HashMap<>();
        if (imageUpdateFlag == true) {
            storageReference = FirebaseStorage.getInstance().getReference().child("UserProfileImages").child(userMobileNumber);
            storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            user.put("phoneNumber",userMobileNumber);
                            user.put("profileImageUri",String.valueOf(uri));
                            user.put("firstName",firstNameEditText.getText().toString());
                            user.put("lastName",lastNameEditText.getText().toString());
                            user.put("email",emailIdEditText.getText().toString().trim());
                            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(UserDetailActivity.this, "1) Account Created successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(UserDetailActivity.this,MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    });
                }
            });

        } else {
            user.put("phoneNumber",userMobileNumber);
            user.put("profileImageUri",profileImageUri);
            user.put("firstName",firstNameEditText.getText().toString());
            user.put("lastName",lastNameEditText.getText().toString());
            user.put("email",emailIdEditText.getText().toString().trim());
            documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(UserDetailActivity.this, "2) Account Created successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(UserDetailActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 1) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
                dpImageView.setImageBitmap(bitmap);
                imageUpdateFlag = true;
            }catch (IOException e) {

            }
        }
    }
}