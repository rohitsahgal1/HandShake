package com.example.handshake;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupDetailsActivity extends AppCompatActivity {

    RecyclerView group_recyclerView;
    ImageView group_dpImageView;
    Button confirmationMassageDialog_YesButton,confirmationMassageDialog_NoButton;
    TextView group_exitGroupTextView,group_deleteGroupTextView,group_groupDescriptionTextView,confirmationMassageDialog_MassageTextView;
    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<UserDetails> userList;
    MyAdapter adapter;

    String groupId;
    String currentUserPhoneNumber;
    String groupCreaterPhoneNumber;

    FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase firebaseDatabase;

    DownloadManager downloadManager;
    long downloadReference;

    Dialog imageDialogPopUp;
    Dialog detailedImageDialog;
    Dialog confirmationMassageDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        getSupportActionBar().hide();

        requestCallPermission();

        groupId = getIntent().getStringExtra("groupId").toString();

        group_recyclerView = (RecyclerView)findViewById(R.id.group_groupMemberRecyclerView);
        group_dpImageView = (ImageView)findViewById(R.id.group_dpImageView);
        group_exitGroupTextView = (TextView)findViewById(R.id.group_exitGroupTextView);
        group_deleteGroupTextView = (TextView)findViewById(R.id.group_deleteGroupTextView);
        group_groupDescriptionTextView = (TextView)findViewById(R.id.group_groupDescriptionTextView);
        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.group_swipeContainer);

        userList = new ArrayList<>();
        adapter = new MyAdapter(this,userList);
        group_recyclerView.setAdapter(adapter);
        group_recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firebaseFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        imageDialogPopUp = new Dialog(this);
        detailedImageDialog = new Dialog(this);
        confirmationMassageDialog = new Dialog(this);

        currentUserPhoneNumber = mAuth.getCurrentUser().getPhoneNumber().toString();

        group_exitGroupTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitFromGroup();
            }
        });

        group_deleteGroupTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                deleteGroup();
                confirmationMassageDialog.setContentView(R.layout.confirmation_popup);
                confirmationMassageDialog.show();
                confirmationMassageDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));
                confirmationMassageDialog_MassageTextView = (TextView)confirmationMassageDialog.findViewById(R.id.confirmationMassage);
                confirmationMassageDialog_YesButton = (Button)confirmationMassageDialog.findViewById(R.id.yes);
                confirmationMassageDialog_NoButton = (Button)confirmationMassageDialog.findViewById(R.id.no);

                confirmationMassageDialog_MassageTextView.setText("Are you sure! You want to Delete the group?");

                confirmationMassageDialog_YesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(GroupDetailsActivity.this, "Deleting....", Toast.LENGTH_SHORT).show();
                        deleteGroup();
                        confirmationMassageDialog.dismiss();
                    }
                });

                confirmationMassageDialog_NoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmationMassageDialog.dismiss();
                    }
                });
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                userList.clear();
                fetchData();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        firebaseFirestore.collection("Group").document(groupId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Glide.with(getApplicationContext()).load(documentSnapshot.getString("groupDpImageUri")).into(group_dpImageView);
                        group_groupDescriptionTextView.setText(documentSnapshot.getString("groupDescription"));
                    }
                });

        fetchData();


    }

    private void deleteGroup() {
        if (currentUserPhoneNumber.equals(groupCreaterPhoneNumber)){
            firebaseFirestore.collection("Group").document(groupId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            firebaseFirestore.collection("Join").whereEqualTo(documentSnapshot.getString("groupName"),groupId)
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                                                Map<String,Object> deleteData = new HashMap<>();
                                                deleteData.put(documentSnapshot.getString("groupName"),FieldValue.delete());
                                                snapshot.getReference().update(deleteData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        documentSnapshot.getReference().delete()
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        firebaseDatabase.getReference().child("Chats")
                                                                                .child(groupId)
                                                                                .removeValue()
                                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                    @Override
                                                                                    public void onSuccess(Void aVoid) {
                                                                                        Toast.makeText(GroupDetailsActivity.this, "Group Deleted SuccessFully", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                        startActivity(new Intent(GroupDetailsActivity.this,MainActivity.class));
                                                                        finish();
                                                                    }
                                                                });
                                                    }
                                                });
                                            }
                                        }
                                    });

                        }
                    });
        }
    }

    private void exitFromGroup() {

        if (!currentUserPhoneNumber.equals(groupCreaterPhoneNumber)) {
            firebaseFirestore.collection("Group").document(groupId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            Map<String,Object> deleteData = new HashMap<>();
                            deleteData.put(documentSnapshot.getString("groupName").toString(), FieldValue.delete());
                            firebaseFirestore.collection("Join").document(currentUserPhoneNumber)
                                    .update(deleteData)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            firebaseDatabase.getReference().child("Chats")
                                                    .child(groupId)
                                                    .child("GroupMembers")
                                                    .child(currentUserPhoneNumber)
                                                    .removeValue();
                                            startActivity(new Intent(GroupDetailsActivity.this,MainActivity.class));
                                            finish();
                                        }
                                    });
                        }
                    });
        }

    }

    private void fetchData() {
        firebaseDatabase.getReference().child("Chats")
                .child(groupId)
                .child("MembersPost")
                .child("Creater")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        groupCreaterPhoneNumber = dataSnapshot.getValue().toString();
                        if (currentUserPhoneNumber.equals(groupCreaterPhoneNumber)) {
                            group_deleteGroupTextView.setVisibility(View.VISIBLE);
                        }
                    }
                });

        firebaseDatabase.getReference().child("Chats")
                .child(groupId)
                .child("GroupMembers")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot:dataSnapshot.getChildren()) {
                            firebaseFirestore.collection("Users").whereEqualTo("phoneNumber",snapshot.getKey().toString()).get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                            List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                                            for (DocumentSnapshot d:list) {
                                                UserDetails obj = d.toObject(UserDetails.class);
                                                userList.add(obj);
                                            }
                                            adapter.notifyDataSetChanged();
                                            swipeRefreshLayout.setRefreshing(false);
                                            group_recyclerView.setAdapter(adapter);
                                        }
                                    });
                        }

                    }
                });
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        Context context;
        ArrayList<UserDetails> userList;

        public MyAdapter(Context context, ArrayList<UserDetails> userList) {
            this.context = context;
            this.userList = userList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.users_custom_design,parent,false);

            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            String userDpUri = userList.get(position).getProfileImageUri();
            Glide.with(context).load(userDpUri).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                    holder.progressBar.setVisibility(View.GONE);
                    return false;
                }
            }).into(holder.user_dpImageView);
            holder.user_nameTextVeiw.setText(userList.get(position).getFirstName()+" "+userList.get(position).getLastName());
            holder.user_phoneNumberTextView.setText(userList.get(position).getPhoneNumber().toString());

            if(groupCreaterPhoneNumber.equals(userList.get(position).getPhoneNumber())) {
                holder.user_postTextView.setText("Creater");
            }

            holder.user_dpImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imageDialogPopUp.setContentView(R.layout.image_view_popup);
                    imageDialogPopUp.show();
                    ProgressBar progressBar = (ProgressBar)imageDialogPopUp.findViewById(R.id.spin_kit);
                    ImageView dp_imageViewPopUp = (ImageView)imageDialogPopUp.findViewById(R.id.imageViewPopUp);
                    Glide.with(context).load(userDpUri).listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    }).into(dp_imageViewPopUp);
                    imageDialogPopUp.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));

                    dp_imageViewPopUp.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            detailedImageDialog.setContentView(R.layout.detailed_image_pop_up);
                            detailedImageDialog.show();
                            ProgressBar progressBar = (ProgressBar)detailedImageDialog.findViewById(R.id.spin_kit);
                            ImageView imageView = (ImageView)detailedImageDialog.findViewById(R.id.imageView);
                            ImageButton back = (ImageButton)detailedImageDialog.findViewById(R.id.back);
                            ImageButton download = (ImageButton)detailedImageDialog.findViewById(R.id.download);
                            Glide.with(GroupDetailsActivity.this).load(userDpUri).listener(new RequestListener<Drawable>() {
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
                                        File path = new File("/HandShake/DP/UserDp");
                                        if (!path.exists()){
                                            path.mkdirs();
                                        }
                                        downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                                        Uri uri = Uri.parse(userDpUri);
                                        DownloadManager.Request request = new DownloadManager.Request(uri);
                                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                                        request.setAllowedOverRoaming(false);
                                        request.setTitle("Photo");
                                        request.setDescription("From HandShake");
                                        request.setDestinationInExternalPublicDir(path.toString(), Calendar.getInstance().getTime().toString());
                                        downloadReference = downloadManager.enqueue(request);
                                        Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show();
                                    }catch (Exception e) {

                                    }
                                }
                            });
                        }
                    });
                }
            });


            holder.usersLinearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (!userList.get(position).getPhoneNumber().equals(currentUserPhoneNumber)) {
                        PopupMenu popupMenu = new PopupMenu(GroupDetailsActivity.this,view);
                        popupMenu.inflate(R.menu.users_popup_menu);
                        popupMenu.show();
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                switch (menuItem.getItemId()) {
                                    case R.id.startChating:
                                        Toast.makeText(context, "chating", Toast.LENGTH_SHORT).show();
                                        return true;
                                    case R.id.call:
                                        requestCallPermission();
                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                        intent.setData(Uri.parse("tel:"+userList.get(position).getPhoneNumber().toString()));
                                        startActivity(intent);
                                        return true;
                                    case R.id.mail:
                                        String[] to = {userList.get(position).getEmail()};
                                        Intent intent1 = new Intent(Intent.ACTION_VIEW);
                                        intent1.setData(Uri.parse("mailto:"));
                                        intent1.putExtra(Intent.EXTRA_EMAIL,to);
                                        startActivity(intent1);
                                        return true;
                                }
                                return false;
                            }
                        });
                    }
                    return false;
                }
            });


        }

        @Override
        public int getItemCount() {
            return userList.size();
        }


        class MyViewHolder extends RecyclerView.ViewHolder {

            LinearLayout usersLinearLayout;
            ImageView user_dpImageView;
            TextView user_nameTextVeiw,user_phoneNumberTextView,user_postTextView;
            ProgressBar progressBar;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);

                usersLinearLayout = (LinearLayout)itemView.findViewById(R.id.usersLinearLayout);
                user_dpImageView = (ImageView)itemView.findViewById(R.id.users_dpImageView);
                user_nameTextVeiw = (TextView)itemView.findViewById(R.id.users_nameTextView);
                user_phoneNumberTextView = (TextView)itemView.findViewById(R.id.users_phoneNumberTextView);
                user_postTextView = (TextView)itemView.findViewById(R.id.users_postTextView);
                progressBar = (ProgressBar)itemView.findViewById(R.id.spin_kit);
            }
        }
    }

    private void requestCallPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CALL_PHONE
                },103);
            }
        }
    }

    private void requestStoragePermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },100);
        }

    }
}