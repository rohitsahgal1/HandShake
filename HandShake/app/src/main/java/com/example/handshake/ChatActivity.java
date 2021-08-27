package com.example.handshake;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    RecyclerView chating_recyclerView;
    EditText chating_EditText;
    ImageButton chating_sentImageButton,chating_cameraImageButton;

    private static final int PICK_IMAGE = 1;

    MyAdapter adapter;
    ArrayList<Massage> massages;

    Uri imageUri;
    Boolean imageFlag = false;

    String subTitleActionBar = "";

    FirebaseAuth mAuth;
    FirebaseDatabase database;
    FirebaseFirestore firebaseFirestore;
    FirebaseStorage firebaseStorage;

    StorageReference storageReference;

    ProgressDialog progressBarDialog;
    Dialog detailedImageDialog;

    DownloadManager downloadManager;
    long downloadReference;



    ImageView groupPopup_dpImageView;
    EditText groupPopup_nameEditText,groupPopup_rangeEditText,groupPopup_descriptionEditText;
    Button groupPopup_createOrUpdateButton;

    Uri imageUriforGroupDp;

    Boolean imageFlagForGroupDp = false;

    FusedLocationProviderClient fusedLocationProviderClient;

    String currentUserMobileNumber;
    String groupCreaterPhoneNumber;
    String groupId;
    String previousGroupId;

    FirebaseDatabase firebaseDatabase;
    DocumentReference documentReference;

    Dialog groupCreateDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getSupportActionBar().setTitle(getIntent().getStringExtra("groupName").toString());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();

        currentUserMobileNumber = mAuth.getCurrentUser().getPhoneNumber().toString();

        massages = new ArrayList<>();
        adapter = new MyAdapter(this,massages);

        chating_recyclerView = (RecyclerView)findViewById(R.id.chating_recyclerView);
        chating_EditText = (EditText)findViewById(R.id.chating_EditText);
        chating_sentImageButton = (ImageButton)findViewById(R.id.chating_sentImageButton);
        chating_cameraImageButton = (ImageButton)findViewById(R.id.chating_cameraImageButton);

        chating_recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chating_recyclerView.setAdapter(adapter);

        progressBarDialog = new ProgressDialog(this);
        detailedImageDialog = new Dialog(this);

        progressBarDialog.show();
        progressBarDialog.setContentView(R.layout.progress_bar_layout);
        ProgressBar progressBar = (ProgressBar)progressBarDialog.findViewById(R.id.spin_kit);
        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);
        progressBarDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));

        database.getReference().child("Chats")
                .child(getIntent().getStringExtra("groupId").toString())
                .child("GroupMembers")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot:dataSnapshot.getChildren()) {
                            subTitleActionBar += "~"+snapshot.getValue() + " , ";
                        }
                        getSupportActionBar().setSubtitle(subTitleActionBar);
                    }
                });

        database.getReference().child("Chats")
                .child(getIntent().getStringExtra("groupId").toString())
                .child("Massages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        massages.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Massage massage = snapshot1.getValue(Massage.class);
                            massage.setChatId(snapshot1.getKey().toString());
                            massages.add(massage);
                        }
                        progressBarDialog.dismiss();
                        adapter.notifyDataSetChanged();

                        chating_recyclerView.smoothScrollToPosition(chating_recyclerView.getAdapter().getItemCount());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        
        chating_cameraImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Pick an image"), PICK_IMAGE);
            }
        });

        chating_sentImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrangeData();
            }
        });
    }

    private void ArrangeData() {
        String massageText = chating_EditText.getText().toString();

        if (!massageText.equals("") || imageFlag) {
            Massage massage = new Massage(massageText,mAuth.getCurrentUser().getPhoneNumber().toString(),Calendar.getInstance().getTime());

            if (imageFlag) {
                storageReference = firebaseStorage.getReference().child("ChatsImages")
                        .child(mAuth.getCurrentUser().getPhoneNumber().toString())
                        .child(Calendar.getInstance().getTime().toString());
                storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                massage.setImages(String.valueOf(uri));
                                progressBarDialog.dismiss();
                                uplaodData(massage,massageText);
                                imageUri = null;
                                imageFlag = false;
                            }
                        });
                    }
                });
            }else {
                massage.setImages("");
                uplaodData(massage,massageText);
            }

            chating_EditText.setText("");

        }
    }

    private void uplaodData(Massage massage,String massageText) {
        database.getReference().child("Chats")
                .child(getIntent().getStringExtra("groupId").toString())
                .child("Massages")
                .push()
                .setValue(massage).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                firebaseFirestore.collection("Users").document(mAuth.getCurrentUser().getPhoneNumber()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String senderName = documentSnapshot.getString("firstName")+" "+documentSnapshot.getString("lastName");
                        Map<String,Object> lastMassage = new HashMap<>();
                        lastMassage.put("groupLastMassage",massageText);
                        lastMassage.put("groupLastMassageSenderName",senderName);
                        lastMassage.put("groupLastMassageDate",Calendar.getInstance().getTime());

                        firebaseFirestore.collection("Group").document(getIntent().getStringExtra("groupId").toString())
                                .update(lastMassage).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });


                    }
                });

            }
        });
    }

    private void exitFromGroup() {
        String gId = getIntent().getStringExtra("groupId").toString();
        firebaseFirestore.collection("Group").document(gId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Map<String,Object> deleteData = new HashMap<>();
                        deleteData.put(documentSnapshot.getString("groupName").toString(), FieldValue.delete());
                        firebaseFirestore.collection("Join").document(currentUserMobileNumber)
                                .update(deleteData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        FirebaseDatabase.getInstance().getReference().child("Chats")
                                                .child(gId)
                                                .child("GroupMembers")
                                                .child(currentUserMobileNumber)
                                                .removeValue();
                                        startActivity(new Intent(ChatActivity.this,MainActivity.class));
                                        finish();
                                    }
                                });
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1) {
            imageUri = data.getData();
            imageFlag = true;
            ArrangeData();
            progressBarDialog.show();
            progressBarDialog.setContentView(R.layout.progress_bar_layout);
            ProgressBar progressBar = (ProgressBar)progressBarDialog.findViewById(R.id.spin_kit);
            Sprite doubleBounce = new DoubleBounce();
            progressBar.setIndeterminateDrawable(doubleBounce);
            progressBarDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));

        }

        if (resultCode == this.RESULT_OK && requestCode == 2) {
            imageUriforGroupDp = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageUriforGroupDp);
                groupPopup_dpImageView.setImageBitmap(bitmap);
                imageFlagForGroupDp = true;
                groupPopup_createOrUpdateButton.setEnabled(true);
            }catch (IOException e) {

            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(ChatActivity.this,MainActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(ChatActivity.this,MainActivity.class));
                finish();
                return true;
            case R.id.more:
                Intent intent = new Intent(ChatActivity.this,GroupDetailsActivity.class);
                intent.putExtra("groupId",getIntent().getStringExtra("groupId").toString());
                startActivity(intent);
                return true;
            case R.id.editGroup:
                groupEditProcess();
                return true;
            case R.id.exit:
                exitFromGroup();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void groupEditProcess() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        groupCreaterPhoneNumber = mAuth.getCurrentUser().getPhoneNumber().toString();

        groupCreateDialog = new Dialog(this);

        groupCreateDialog.setContentView(R.layout.group_create_popup);
        groupCreateDialog.show();
        groupCreateDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
        groupPopup_dpImageView = (ImageView)groupCreateDialog.findViewById(R.id.groupPopup_dpImageView);
        groupPopup_nameEditText = (EditText)groupCreateDialog.findViewById(R.id.groupPopup_nameEditText);
        groupPopup_rangeEditText = (EditText)groupCreateDialog.findViewById(R.id.groupPopup_rangeEditText);
        groupPopup_descriptionEditText = (EditText)groupCreateDialog.findViewById(R.id.groupPopup_descriptionEditText);
        groupPopup_createOrUpdateButton = (Button)groupCreateDialog.findViewById(R.id.groupPopup_createOrUpdateButton);

        groupPopup_dpImageView.setEnabled(false);
        groupPopup_nameEditText.setEnabled(false);
        groupPopup_rangeEditText.setEnabled(false);
        groupPopup_descriptionEditText.setEnabled(false);
        groupPopup_createOrUpdateButton.setText("Update");
        groupPopup_createOrUpdateButton.setEnabled(false);

        fetchDataForGroupEdit();

        groupPopup_dpImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Pick an image"), 2);
            }
        });

        groupPopup_createOrUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationProviderClient.getLastLocation()
                                .addOnSuccessListener(new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        if (location != null) {
                                            Double lat = location.getLatitude();
                                            Double longt = location.getLongitude();

                                            if (!groupPopup_nameEditText.getText().toString().equals("")) {
                                                uploadDataForGroupEdit(lat,longt);
                                                Toast.makeText(ChatActivity.this, "Uploading Start......", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(ChatActivity.this, "Please Give the Group Name", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                });
                    } else {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},3);
                    }
                }
            }
        });
    }

    private void uploadDataForGroupEdit(Double lat, Double longt) {
        documentReference = firebaseFirestore.collection("Group").document(getIntent().getStringExtra("groupId").toString());
        if (imageFlagForGroupDp) {
            storageReference = FirebaseStorage.getInstance().getReference().child("groupDpImage").child(groupPopup_nameEditText.getText().toString()+groupCreaterPhoneNumber);
            storageReference.putFile(imageUriforGroupDp).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
//                            CreateGroup createGroup = new CreateGroup(String.valueOf(uri),groupPopup_nameEditText.getText().toString(),groupPopup_descriptionEditText.getText().toString(),groupCreaterPhoneNumber,longt,lat,documentReference.getId());
//                            createGroup.setGroupRange(groupPopup_rangeEditText.getText().toString());
                            Map<String,Object> updateGroup = new HashMap<>();
                            updateGroup.put("groupDpImageUri",String.valueOf(uri));
                            updateGroup.put("groupName",groupPopup_nameEditText.getText().toString());
                            updateGroup.put("groupRange",groupPopup_rangeEditText.getText().toString());
                            updateGroup.put("groupDescription",groupPopup_descriptionEditText.getText().toString());

                            documentReference.update(updateGroup).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    imageFlagForGroupDp = false;
                                    Toast.makeText(ChatActivity.this, "Group Successfully updated", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
            });
        } else {
//            CreateGroup createGroup = new CreateGroup(String.valueOf(imageUriforGroupDp),groupPopup_nameEditText.getText().toString(),groupPopup_descriptionEditText.getText().toString(),groupCreaterPhoneNumber,longt,lat,documentReference.getId());
//            createGroup.setGroupRange(groupPopup_rangeEditText.getText().toString());
            Map<String,Object> updateGroup = new HashMap<>();
            updateGroup.put("groupDpImageUri",String.valueOf(imageUriforGroupDp));
            updateGroup.put("groupName",groupPopup_nameEditText.getText().toString());
            updateGroup.put("groupRange",groupPopup_rangeEditText.getText().toString());
            updateGroup.put("groupDescription",groupPopup_descriptionEditText.getText().toString());

            documentReference.update(updateGroup).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(ChatActivity.this, "Group Successfully updated", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchDataForGroupEdit() {
        documentReference = firebaseFirestore.collection("Group").document(getIntent().getStringExtra("groupId").toString());
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                CreateGroup data = documentSnapshot.toObject(CreateGroup.class);
                imageUriforGroupDp = Uri.parse(data.getGroupDpImageUri());
                Glide.with(ChatActivity.this).load(data.getGroupDpImageUri()).into(groupPopup_dpImageView);
                groupPopup_nameEditText.setText(data.getGroupName());
                groupPopup_rangeEditText.setText(data.getGroupRange());
                groupPopup_descriptionEditText.setText(data.getGroupDescription());

                groupPopup_dpImageView.setEnabled(true);
                groupPopup_nameEditText.setEnabled(true);
                groupPopup_rangeEditText.setEnabled(true);
                groupPopup_descriptionEditText.setEnabled(true);
                groupPopup_createOrUpdateButton.setEnabled(true);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        FirebaseDatabase.getInstance().getReference().child("Chats")
                .child(getIntent().getStringExtra("groupId").toString())
                .child("MembersPost")
                .child("Creater")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        getMenuInflater().inflate(R.menu.chat_menu,menu);
                        String groupCreaterMobileNumber = dataSnapshot.getValue().toString();
                        if (currentUserMobileNumber.equals(groupCreaterMobileNumber)) {
                            groupCreaterPhoneNumber = groupCreaterMobileNumber;
                            menu.getItem(2).setVisible(false);
                        }
                        if (!currentUserMobileNumber.equals(groupCreaterMobileNumber)){
                            menu.getItem(0).setVisible(false);
                        }
                    }
                });
        return super.onCreateOptionsMenu(menu);
    }

    class MyAdapter extends RecyclerView.Adapter {

        Context context;
        ArrayList<Massage> massages;

        final int ITEM_SENT = 1;
        final int ITEM_RECIEVED = 2;

        public MyAdapter(Context context, ArrayList<Massage> massages) {
            this.context = context;
            this.massages = massages;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == ITEM_SENT) {
                View view = LayoutInflater.from(context).inflate(R.layout.sent_massage_layout,parent,false);
                return new SentViewHolder(view);
            } else {
                View view = LayoutInflater.from(context).inflate(R.layout.recieved_massage_layout, parent, false);
                return new RecievedViewHolder(view);
            }
        }

        @Override
        public int getItemViewType(int position) {
            Massage massage = massages.get(position);
            if(mAuth.getCurrentUser().getPhoneNumber().toString().equals(massage.getPhoneNumber())) {
                return ITEM_SENT;
            } else {
                return ITEM_RECIEVED;
            }

        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Massage massage = massages.get(position);
            DateFormat df = new SimpleDateFormat("h:mm a");
            if (holder.getClass() == SentViewHolder.class) {
                SentViewHolder viewHolder = (SentViewHolder)holder;
                try {
                    if (!massage.getMassages().equals("")) {
                        viewHolder.sentMassageTextView.setVisibility(View.VISIBLE);
                        viewHolder.sentMassageTextView.setText(massage.getMassages());
                    }
                    else {
                        viewHolder.sentMassageTextView.setVisibility(View.GONE);
                    }
                }catch (Exception e){

                }

                viewHolder.sent_timeStampTextView.setText(df.format(massage.getTimeStamp()));

                try {
                    if (!massage.getImages().equals("")){
                        viewHolder.sentMassageCardView.setVisibility(View.VISIBLE);
                        Glide.with(context).load(massage.getImages()).into(viewHolder.sentMassageImageView);
                    }
                    else {
                        viewHolder.sentMassageCardView.setVisibility(View.GONE);
                    }
                } catch (Exception e){

                }

                viewHolder.sentMassageImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        detailedImageDialog.setContentView(R.layout.detailed_image_pop_up);
                        detailedImageDialog.show();
                        ProgressBar progressBar = (ProgressBar)detailedImageDialog.findViewById(R.id.spin_kit);
                        ImageView imageView = (ImageView)detailedImageDialog.findViewById(R.id.imageView);
                        ImageButton back = (ImageButton)detailedImageDialog.findViewById(R.id.back);
                        ImageButton download = (ImageButton)detailedImageDialog.findViewById(R.id.download);
                        Glide.with(ChatActivity.this).load(massage.getImages()).listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        }).into(imageView);
                        detailedImageDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));

                        back.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                detailedImageDialog.dismiss();
                            }
                        });

                        download.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                requestStoragePermission();
                                try {
                                    File path = new File("/HandShake/Photos/Sent");
                                    if (!path.exists()){
                                        path.mkdirs();
                                    }
                                    downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                                    Uri uri = Uri.parse(massage.getImages());
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                                    request.setAllowedOverRoaming(false);
                                    request.setTitle("Photo");
                                    request.setDescription("From HandShake");
                                    request.setDestinationInExternalPublicDir(path.toString(),Calendar.getInstance().getTime().toString());
                                    downloadReference = downloadManager.enqueue(request);
                                    Toast.makeText(ChatActivity.this, "Download Started", Toast.LENGTH_SHORT).show();
                                }catch (Exception e) {

                                }
                            }
                        });

                    }
                });

                viewHolder.sent_constraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        deleteMassage(view,massage);
                        return false;
                    }
                });

               viewHolder.sentMassageImageView.setOnLongClickListener(new View.OnLongClickListener() {
                   @Override
                   public boolean onLongClick(View view) {
                       deleteMassage(view,massage);
                       return false;
                   }
               });

            } else {
                RecievedViewHolder viewHolder = (RecievedViewHolder)holder;
                try {
                    if (!massage.getMassages().equals("")) {
                        viewHolder.recievedMassageTextView.setVisibility(View.VISIBLE);
                        viewHolder.recievedMassageTextView.setText(massage.getMassages());
                    }
                    else {
                        viewHolder.recievedMassageTextView.setVisibility(View.GONE);
                    }
                } catch (Exception e) {

                }

                viewHolder.recieved_timeStampTextView.setText(df.format(massage.getTimeStamp()));

                try {
                    if (!massage.getImages().equals("")){
                        viewHolder.recievedMassageCardView.setVisibility(View.VISIBLE);
                        Glide.with(context).load(massage.getImages()).into(viewHolder.recievedMassageImageView);
                    }
                    else {
                        viewHolder.recievedMassageCardView.setVisibility(View.GONE);
                    }
                } catch (Exception e) {

                }


                viewHolder.recieved_constraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if(currentUserMobileNumber.equals(groupCreaterPhoneNumber)) {
                            deleteMassage(view,massage);
                        }
                        return false;
                    }
                });

                viewHolder.recievedMassageImageView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if(currentUserMobileNumber.equals(groupCreaterPhoneNumber)) {
                            deleteMassage(view,massage);
                        }
                        return false;
                    }
                });

                viewHolder.recievedMassageImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        detailedImageDialog.setContentView(R.layout.detailed_image_pop_up);
                        detailedImageDialog.show();
                        ProgressBar progressBar = (ProgressBar)detailedImageDialog.findViewById(R.id.spin_kit);
                        ImageView imageView = (ImageView)detailedImageDialog.findViewById(R.id.imageView);
                        ImageButton back = (ImageButton)detailedImageDialog.findViewById(R.id.back);
                        ImageButton download = (ImageButton)detailedImageDialog.findViewById(R.id.download);
                        Glide.with(ChatActivity.this).load(massage.getImages()).listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }
                        }).into(imageView);
                        detailedImageDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));

                        back.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                detailedImageDialog.dismiss();
                            }
                        });

                        download.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                requestStoragePermission();
                                try {
                                    File path = new File(Environment.getExternalStorageDirectory()+"/HandShake/Photos");
                                    if (!path.exists()){
                                        path.mkdir();
                                    }
                                    downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                                    Uri uri = Uri.parse(massage.getImages());
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                                    request.setAllowedOverRoaming(false);
                                    request.setTitle("Photo");
                                    request.setDescription("From HandShake");
                                    request.setDestinationInExternalPublicDir(path.toString(),Calendar.getInstance().getTime().toString());
                                    downloadReference = downloadManager.enqueue(request);
                                    Toast.makeText(ChatActivity.this, "Download Started", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {

                                }

                            }
                        });
                    }
                });

                FirebaseFirestore.getInstance().collection("Users").document(massage.getPhoneNumber()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        viewHolder.recieved_userNameTextView.setText("~"+documentSnapshot.getString("firstName")+" "+documentSnapshot.getString("lastName"));
                    }
                });
            }
        }

        private void deleteMassage(View view,Massage massage) {
            PopupMenu popupMenu = new PopupMenu(ChatActivity.this,view);
            popupMenu.inflate(R.menu.massage_popup);
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.delete:
                            FirebaseDatabase.getInstance().getReference().child("Chats")
                                    .child(getIntent().getStringExtra("groupId").toString())
                                    .child("Massages")
                                    .child(massage.getChatId())
                                    .removeValue();

                            return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return massages.size();
        }

        class SentViewHolder extends RecyclerView.ViewHolder {

            TextView sentMassageTextView,sent_timeStampTextView;
            ImageView sentMassageImageView;
            CardView sentMassageCardView;
            ConstraintLayout sent_constraintLayout;

            public SentViewHolder(@NonNull View itemView) {
                super(itemView);

                sentMassageTextView = (TextView)itemView.findViewById(R.id.sentMassageTextView);
                sent_timeStampTextView = (TextView)itemView.findViewById(R.id.sent_timeStampTextView);
                sentMassageImageView = (ImageView)itemView.findViewById(R.id.sentMassageImageView);
                sentMassageCardView = (CardView)itemView.findViewById(R.id.sentMassageCardView);
                sent_constraintLayout = (ConstraintLayout)itemView.findViewById(R.id.sent_constraintLayout);
            }
        }

        class RecievedViewHolder extends RecyclerView.ViewHolder {

            TextView recievedMassageTextView,recieved_timeStampTextView,recieved_userNameTextView;
            ImageView recievedMassageImageView;
            CardView recievedMassageCardView;
            ConstraintLayout recieved_constraintLayout;

            public RecievedViewHolder(@NonNull View itemView) {
                super(itemView);

                recievedMassageTextView = (TextView)itemView.findViewById(R.id.recievedMassageTextView);
                recieved_timeStampTextView = (TextView)itemView.findViewById(R.id.recieved_timeStampTextView);
                recieved_userNameTextView = (TextView)itemView.findViewById(R.id.recieved_userNameTextView);
                recievedMassageImageView = (ImageView)itemView.findViewById(R.id.recievedMassageImageView);
                recievedMassageCardView = (CardView)itemView.findViewById(R.id.recievedMassageCardView);
                recieved_constraintLayout = (ConstraintLayout)itemView.findViewById(R.id.recieved_constraintLayout);
            }
        }
    }

    private void requestStoragePermission() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },100);
        }

    }
}