package com.example.handshake;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.handshake.ui.main.SectionsPagerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ImageView groupPopup_dpImageView;
    EditText groupPopup_nameEditText,groupPopup_rangeEditText,groupPopup_descriptionEditText;
    TextView textView;
    Button groupPopup_createOrUpdateButton;

    Uri imageUri;

    FusedLocationProviderClient fusedLocationProviderClient;

    String groupCreaterPhoneNumber;
    String groupId;

    FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase firebaseDatabase;
    DocumentReference documentReference;
    StorageReference storageReference;

    FloatingActionButton fab;
    Dialog groupCreateDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        groupCreaterPhoneNumber = mAuth.getCurrentUser().getPhoneNumber().toString();

        groupCreateDialog = new Dialog(this);

        fab = (FloatingActionButton)findViewById(R.id.fab);

        firebaseFirestore.collection("Users").document(mAuth.getCurrentUser().getPhoneNumber().toString()).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {

                        } else {
                            startActivity(new Intent(MainActivity.this,UserDetailActivity.class));
                            finish();
                        }
                    }
                });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                groupCreateDialog.setContentView(R.layout.group_create_popup);
                groupCreateDialog.show();
                groupCreateDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
                groupPopup_dpImageView = (ImageView)groupCreateDialog.findViewById(R.id.groupPopup_dpImageView);
                groupPopup_nameEditText = (EditText)groupCreateDialog.findViewById(R.id.groupPopup_nameEditText);
                groupPopup_rangeEditText = (EditText)groupCreateDialog.findViewById(R.id.groupPopup_rangeEditText);
                groupPopup_descriptionEditText = (EditText)groupCreateDialog.findViewById(R.id.groupPopup_descriptionEditText);
                groupPopup_createOrUpdateButton = (Button)groupCreateDialog.findViewById(R.id.groupPopup_createOrUpdateButton);
                textView = (TextView)groupCreateDialog.findViewById(R.id.textView);

                groupPopup_dpImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent,"Pick an image"), 1);
                    }
                });

                groupPopup_createOrUpdateButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        groupPopup_createOrUpdateButton.setEnabled(false);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                            if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationProviderClient.getLastLocation()
                                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                                            @Override
                                            public void onSuccess(Location location) {
                                                if (location != null) {
                                                    Double lat = location.getLatitude();
                                                    Double longt = location.getLongitude();

                                                    if (!groupPopup_nameEditText.getText().toString().equals("")) {
                                                        Toast.makeText(MainActivity.this, "Creating ......", Toast.LENGTH_SHORT).show();
                                                        uploadData(lat,longt);
                                                    } else {
                                                        Toast.makeText(MainActivity.this, "Please Give the Group Name", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }
                                        });
                            } else {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},2);
                            }
                        }
                    }
                });
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_OK && requestCode == 1) {
            imageUri = data.getData();
            textView.setVisibility(View.GONE);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUri);
                groupPopup_dpImageView.setImageBitmap(bitmap);
                groupPopup_createOrUpdateButton.setEnabled(true);
            }catch (IOException e) {

            }
        }
    }

    private void uploadData(Double latitude, Double longitude) {
        documentReference = firebaseFirestore.collection("Group").document();
        storageReference = FirebaseStorage.getInstance().getReference().child("groupDpImage").child(groupPopup_nameEditText.getText().toString()+groupCreaterPhoneNumber);
        storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        CreateGroup createGroup = new CreateGroup(String.valueOf(uri),groupPopup_nameEditText.getText().toString(),groupPopup_descriptionEditText.getText().toString(),groupCreaterPhoneNumber,longitude,latitude,documentReference.getId());
                        createGroup.setGroupRange(groupPopup_rangeEditText.getText().toString());
                        documentReference.set(createGroup).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                groupId = documentReference.getId();
                                documentReference = firebaseFirestore.collection("Join").document(groupCreaterPhoneNumber);
                                Map<String,Object> group = new HashMap<>();
                                group.put(groupPopup_nameEditText.getText().toString(),groupId);
                                documentReference.set(group, SetOptions.merge());

                                firebaseFirestore.collection("Users").document(groupCreaterPhoneNumber).get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                Map<String,Object> member = new HashMap<>();
                                                member.put(groupCreaterPhoneNumber.toString(),documentSnapshot.getString("firstName")+" "+documentSnapshot.getString("lastName"));
                                                firebaseDatabase.getReference().child("Chats")
                                                        .child(groupId)
                                                        .child("GroupMembers")
                                                        .setValue(member);

                                                Map<String,Object> postDetails = new HashMap<>();
                                                postDetails.put("Creater",groupCreaterPhoneNumber);
                                                firebaseDatabase.getReference().child("Chats")
                                                        .child(groupId)
                                                        .child("MembersPost")
                                                        .setValue(postDetails);
                                            }
                                        });
                                groupCreateDialog.dismiss();
                                Toast.makeText(MainActivity.this, "Group Successfully created", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }
}