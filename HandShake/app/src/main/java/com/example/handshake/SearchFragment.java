package com.example.handshake;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class SearchFragment extends Fragment {

    RecyclerView search_recyclerView;
    SwipeRefreshLayout swipeRefreshLayout;

    ArrayList<CreateGroup> groupDataList;
    MyAdapter adapter;

    String currentUserPhoneNumber;
    Double currentUserLongitude;
    Double currentUserLatitude;

    FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseDatabase firebaseDatabase;

    DocumentReference documentReference;

    Dialog imageDialogPopUp;
    ProgressDialog progressBarDialog;

    FusedLocationProviderClient fusedLocationProviderClient;

    LocationManager locationManager;

    SharedPreferences sharedPreferences = null;
    Boolean nightModeFlag;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.search_fragment_layout,container,false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);

        search_recyclerView = (RecyclerView)v.findViewById(R.id.search_recyclerView);
        search_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        swipeRefreshLayout = (SwipeRefreshLayout)v.findViewById(R.id.search_swipeContainer);

        groupDataList = new ArrayList<>();
        adapter = new MyAdapter(groupDataList);
        search_recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        imageDialogPopUp = new Dialog(getContext());
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
        requestLocationPermission();
        locationPermission();
        firebaseFirestore.collection("Group").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot d:list) {
                            CreateGroup obj = d.toObject(CreateGroup.class);

                            try {
                                Double distance = haversine(currentUserLatitude,currentUserLongitude,obj.getGroupLatitude(),obj.getGroupLongitude());
                                if (obj.getGroupRange().equals("") || obj.getGroupRange().equals(null)) {
                                    groupDataList.add(obj);
                                } else if (Double.parseDouble(obj.getGroupRange()) >= distance){
                                    groupDataList.add(obj);
                                }
                            } catch (Exception e) {

                            }

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

    public Double haversine(double lat1, double lon1,
                          double lat2, double lon2)
    {
        // distance between latitudes and longitudes
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // convert to radians
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // apply formulae
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(lat1) *
                        Math.cos(lat2);
        double rad = 6371;
        double c = 2 * Math.asin(Math.sqrt(a));
        return rad * c * 1000;
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_custom_layout,parent,false);

            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
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
            }).into(holder.search_dpImageView);
            holder.search_groupNameTextView.setText(groupDataList.get(position).getGroupName());
            holder.search_groupDescriptionTextView.setText(groupDataList.get(position).getGroupDescription());

            holder.search_dpImageView.setOnClickListener(new View.OnClickListener() {
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

                }
            });

            holder.search_joinTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    documentReference = firebaseFirestore.collection("Join").document(mAuth.getCurrentUser().getPhoneNumber().toString());
                    Map<String,Object> group = new HashMap<>();
                    group.put(groupDataList.get(position).getGroupName().toString(),groupDataList.get(position).getGroupId());
                    documentReference.set(group, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getActivity(), "You Successfully Joined the Group", Toast.LENGTH_SHORT).show();
                        }
                    });

                    firebaseFirestore.collection("Users").document(currentUserPhoneNumber).get()
                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    Map<String,Object> member = new HashMap<>();
                                    member.put(currentUserPhoneNumber,documentSnapshot.getString("firstName")+" "+documentSnapshot.getString("lastName"));
                                    firebaseDatabase.getReference().child("Chats")
                                            .child(groupDataList.get(position).getGroupId())
                                            .child("GroupMembers")
                                            .updateChildren(member);
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

            ImageView search_dpImageView;
            TextView search_groupNameTextView,search_groupDescriptionTextView,search_joinTextView;
            ProgressBar progressBar;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);

                search_dpImageView = (ImageView)itemView.findViewById(R.id.search_dpImageView);
                search_groupNameTextView = (TextView)itemView.findViewById(R.id.search_groupNameTextView);
                search_groupDescriptionTextView = (TextView)itemView.findViewById(R.id.search_groupDescriptionTextView);
                search_joinTextView = (TextView)itemView.findViewById(R.id.search_joinTextView);
                progressBar = (ProgressBar)itemView.findViewById(R.id.spin_kit);
            }
        }
    }

    public void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                currentUserLatitude = location.getLatitude();
                                currentUserLongitude = location.getLongitude();
                            }
                        }
                    });

        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},getTargetRequestCode());
        }
    }

    public void locationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location locNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location locGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locPassive = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (locNetwork != null) {
                currentUserLatitude = locNetwork.getLatitude();
                currentUserLongitude = locNetwork.getLongitude();
            } else if (locGPS != null) {
                currentUserLatitude = locGPS.getLatitude();
                currentUserLongitude = locGPS.getLongitude();
            } else if (locPassive != null) {
                currentUserLatitude = locPassive.getLatitude();
                currentUserLongitude = locPassive.getLongitude();
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},200);
        }
    }
}
