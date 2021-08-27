package com.example.handshake;

import android.Manifest;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class ChatsFragment extends Fragment {

    RecyclerView chats_recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;

    Object[] joinedGroupIdList;
    ArrayList<CreateGroup> groupDataList;
    String currentUserPhoneNumber;
    String currentUserName;
    String txt;

    MyAdapter adapter;

    FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase database;

    DownloadManager downloadManager;
    long downloadReference;

    Dialog imageDialogPopUp;
    Dialog detailedImageDialog;
    ProgressDialog progressBarDialog;

    SharedPreferences sharedPreferences = null;
    Boolean nightModeFlag;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.chat_fragment_layout,container,false);

        chats_recyclerView = (RecyclerView)v.findViewById(R.id.chats_recyclerView);
        chats_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        swipeRefreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.chats_swipeContainer);

        joinedGroupIdList = new ArrayList().toArray();
        groupDataList = new ArrayList<>();
        adapter = new MyAdapter(groupDataList);
        chats_recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance();

        imageDialogPopUp = new Dialog(getContext());
        detailedImageDialog = new Dialog(getContext());
        progressBarDialog = new ProgressDialog(getContext());

        progressBarDialog.show();
        progressBarDialog.setContentView(R.layout.progress_bar_layout);
        ProgressBar progressBar = (ProgressBar)progressBarDialog.findViewById(R.id.spin_kit);
        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);
        progressBarDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0,0,0,0)));

        currentUserPhoneNumber = mAuth.getCurrentUser().getPhoneNumber().toString();



        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                groupDataList.clear();
                fetchData();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        fetchData();

        return v;
    }

    private void fetchData() {

        firebaseFirestore.collection("Join").document(currentUserPhoneNumber).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        try{
                            joinedGroupIdList = documentSnapshot.getData().values().toArray();

                            for (Object data:joinedGroupIdList) {
                                firebaseFirestore.collection("Group").whereEqualTo("groupId",data.toString()).get()
                                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                            @Override
                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                                                for (DocumentSnapshot d:list) {
                                                    CreateGroup obj = d.toObject(CreateGroup.class);
                                                    groupDataList.add(obj);
                                                }
                                                progressBarDialog.dismiss();
                                                adapter.notifyDataSetChanged();
                                                swipeRefreshLayout.setRefreshing(false);
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressBarDialog.dismiss();
                                    }
                                });

                            }
                        } catch (Exception e) {
                            progressBarDialog.dismiss();
                        }

                    }
                });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu,menu);
        super.onCreateOptionsMenu(menu, inflater);
        sharedPreferences = getActivity().getSharedPreferences("night",0);
        nightModeFlag = sharedPreferences.getBoolean("night_mode",true);
        if (nightModeFlag) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            menu.getItem(2).setChecked(true);
        }

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
            case R.id.nightMode:
                if (item.isChecked()){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("night_mode",false);
                    editor.commit();
                    item.setChecked(false);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("night_mode",true);
                    editor.commit();
                    item.setChecked(true);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }



    class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        ArrayList<CreateGroup> groupDataList;

        public MyAdapter(ArrayList<CreateGroup> groupDataList) {
            this.groupDataList = groupDataList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chats_custom_layout,parent,false);

            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
                DateFormat df = new SimpleDateFormat("h:mm a");
                String groupDpUri = groupDataList.get(position).getGroupDpImageUri();
                Glide.with(getContext()).load(groupDpUri).listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                }).into(holder.dpImageView);
                holder.groupNameTextView.setText(groupDataList.get(position).getGroupName());
//                holder.lastMassageTextView.setText(groupDataList.get(position).getGroupDescription());

                holder.lastMassageTextView.setText("~"+groupDataList.get(position).getGroupLastMassageSenderName()+" : "+groupDataList.get(position).getGroupLastMassage());
                try {
                    holder.chats_lastMassageTime.setText(df.format(groupDataList.get(position).getGroupLastMassageDate()));
                }catch (Exception e) {

                }


                holder.chatsLinearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(),ChatActivity.class);
                        intent.putExtra("groupName",holder.groupNameTextView.getText().toString());
                        intent.putExtra("groupId",groupDataList.get(position).getGroupId().toString());
                        startActivity(intent);
                        getActivity().finish();
                    }
                });

                holder.dpImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        imageDialogPopUp.setContentView(R.layout.image_view_popup);
                        imageDialogPopUp.show();
                        ProgressBar progressBar = (ProgressBar)imageDialogPopUp.findViewById(R.id.spin_kit);
                        ImageView dp_imageViewPopUp = (ImageView)imageDialogPopUp.findViewById(R.id.imageViewPopUp);
                        Glide.with(getContext()).load(groupDpUri).listener(new RequestListener<Drawable>() {
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
                                Glide.with(getActivity()).load(groupDpUri).listener(new RequestListener<Drawable>() {
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
                                            File path = new File("/HandShake/DP/GroupDp");
                                            if (!path.exists()){
                                                path.mkdirs();
                                            }
                                            downloadManager = (DownloadManager)getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                            Uri uri = Uri.parse(groupDpUri);
                                            DownloadManager.Request request = new DownloadManager.Request(uri);
                                            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                                            request.setAllowedOverRoaming(false);
                                            request.setTitle("Photo");
                                            request.setDescription("From HandShake");
                                            request.setDestinationInExternalPublicDir(path.toString(), Calendar.getInstance().getTime().toString());
                                            downloadReference = downloadManager.enqueue(request);
                                            Toast.makeText(getContext(), "Download Started", Toast.LENGTH_SHORT).show();
                                        }catch (Exception e) {

                                        }
                                    }
                                });

                            }
                        });
                    }
                });
        }

        @Override
        public int getItemCount() {
            return groupDataList.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            ImageView dpImageView;
            TextView groupNameTextView,lastMassageTextView,chats_lastMassageTime;
            LinearLayout chatsLinearLayout;
            ProgressBar progressBar;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);

                dpImageView = (ImageView)itemView.findViewById(R.id.dpImageView);
                groupNameTextView = (TextView)itemView.findViewById(R.id.groupNameTextView);
                lastMassageTextView = (TextView)itemView.findViewById(R.id.lastMassageTextView);
                chatsLinearLayout = (LinearLayout)itemView.findViewById(R.id.chatLinearLayout);
                chats_lastMassageTime = (TextView)itemView.findViewById(R.id.chats_lastMassageTime);
                progressBar = (ProgressBar)itemView.findViewById(R.id.spin_kit);
            }
        }
    }

    private void requestStoragePermission() {

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            },100);
        }

    }
}
