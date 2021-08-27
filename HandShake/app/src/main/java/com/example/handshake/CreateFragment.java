package com.example.handshake;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
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
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CreateFragment extends Fragment {

    ImageView groupDpImageView;
    EditText groupNameEditText,descriptionEditText;
    Button createGroupButton;

    Uri imageUri;
    private static final int PICK_IMAGE = 1;

    FusedLocationProviderClient fusedLocationProviderClient;
    
    String groupCreaterPhoneNumber;
    String groupId;

    FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase firebaseDatabase;
    DocumentReference documentReference;

    StorageReference storageReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.create_fragment_layout,container,false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());

        groupDpImageView = (ImageView)v.findViewById(R.id.groupDpImageView);
        groupNameEditText = (EditText)v.findViewById(R.id.groupNameEditText);
        descriptionEditText = (EditText)v.findViewById(R.id.descriptionEditText);
        createGroupButton = (Button)v.findViewById(R.id.createGroupButton);
        
        createGroupButton.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        groupCreaterPhoneNumber = mAuth.getCurrentUser().getPhoneNumber().toString();
        
        createGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if (getActivity().getApplicationContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationProviderClient.getLastLocation()
                                .addOnSuccessListener(new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        if (location != null) {
                                            Double lat = location.getLatitude();
                                            Double longt = location.getLongitude();

                                            if (!groupNameEditText.getText().toString().equals("")) {
                                                uploadData(lat,longt);
                                            } else {
                                                Toast.makeText(getActivity(), "Please Give the Group Name", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                });
                    } else {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},getTargetRequestCode());
                    }
                }
            }
        });

        groupDpImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Pick an image"), PICK_IMAGE);
            }
        });


        return v;
    }

    private void uploadData(Double latitude, Double longitude) {
        documentReference = firebaseFirestore.collection("Group").document();
        storageReference = FirebaseStorage.getInstance().getReference().child("groupDpImage").child(groupNameEditText.getText().toString()+groupCreaterPhoneNumber);
        storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {

                        CreateGroup createGroup = new CreateGroup(String.valueOf(uri),groupNameEditText.getText().toString(),descriptionEditText.getText().toString(),groupCreaterPhoneNumber,longitude,latitude,documentReference.getId());
                        documentReference.set(createGroup).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                groupId = documentReference.getId();
                                documentReference = firebaseFirestore.collection("Join").document(groupCreaterPhoneNumber);
                                Map<String,Object> group = new HashMap<>();
                                group.put(groupNameEditText.getText().toString(),groupId);
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
                                Toast.makeText(getActivity(), "Group Successfully created", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK && requestCode == 1) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(),imageUri);
                groupDpImageView.setImageBitmap(bitmap);
                createGroupButton.setEnabled(true);
            }catch (IOException e) {

            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu,menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logoutMenuItem:
                FirebaseAuth.getInstance().signOut();
                getActivity().finish();
                break;
            case R.id.profile:
                startActivity(new Intent(getActivity(),UserDetailActivity.class));
                getActivity().finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
