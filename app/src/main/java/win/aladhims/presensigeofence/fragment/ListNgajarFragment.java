package win.aladhims.presensigeofence.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;
import win.aladhims.presensigeofence.DetailNgajarActivity;
import win.aladhims.presensigeofence.Model.MahasiswaPresent;
import win.aladhims.presensigeofence.Model.Ngajar;
import win.aladhims.presensigeofence.R;
import win.aladhims.presensigeofence.ViewHolder.ListNgajarViewHolder;
import win.aladhims.presensigeofence.ViewHolder.ListNgajarkuViewHolder;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;


public class ListNgajarFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener,GoogleApiClient.ConnectionCallbacks, EasyPermissions.PermissionCallbacks{

    private static final String TAG = ListNgajarFragment.class.getSimpleName();
    private DatabaseReference mDatabaseReference,ngajarRef,locRef;
    private FirebaseRecyclerAdapter<Ngajar,ListNgajarViewHolder> mAdapter;
    private RecyclerView mRecyclerView;
    private ProgressDialog mProgressDialog;
    private static final int RC_PERMS_FINELOC = 1441;
    private static final String EXTRA_FROM_LISTNGAJAR = "NGAJAR";

    String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
    private String key;
    private View rootView;

    private GoogleApiClient mGoogleApiClient;

    public ListNgajarFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.list_ngajar,container,false);

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        ngajarRef = mDatabaseReference.child("ikutngajar-mahasiswa");
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.rv_list_ngajar);

        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        lm.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(lm);

        mAdapter = new FirebaseRecyclerAdapter<Ngajar, ListNgajarViewHolder>(Ngajar.class,R.layout.list_ngajar_item,ListNgajarViewHolder.class,ngajarRef) {
            @Override
            protected void populateViewHolder(final ListNgajarViewHolder viewHolder, Ngajar model, int position) {
                key = getRef(position).getKey();
                mDatabaseReference.child("ngajar").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Ngajar ngajar = dataSnapshot.getValue(Ngajar.class);

                        locRef = mDatabaseReference.child("ikutngajar-mahasiswa").child(key);
                        Log.d(TAG, key);

                        Glide.with(getActivity())
                                .load(ngajar.getPhotoURLDosen())
                                .into(viewHolder.mIvFotoDosen);

                        viewHolder.mTvNamaDosen.setText(ngajar.getNamaDosen());
                        viewHolder.mTvEmailDosen.setText(ngajar.getEmailDosen());
                        viewHolder.mTvKontakDosen.setText(ngajar.getKontakDosen());
                        viewHolder.mTvNamaMatkul.setText(ngajar.getNamaMatkul());
                        viewHolder.mTvWaktuMulai.setText(ngajar.getHari()+ ", " + ngajar.makeJamNgajar());
                        viewHolder.mTvKelas.setText(ngajar.getKelasDiajar());
                        viewHolder.mTvDurasi.setText(String.valueOf(ngajar.getDurasiNgajar()));
                        viewHolder.mTvJumlahBintang.setText(String.valueOf(ngajar.getJumlahStar()));

                        viewHolder.mBtnIkut.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ikutMatkul(key);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }
        };
        mRecyclerView.setAdapter(mAdapter);
        return rootView;

    }

    public void ikutMatkul(final String k){
        if (EasyPermissions.hasPermissions(getActivity(), perms)) {
            GeoFire ngajarLoc = new GeoFire(locRef);

            final Location mahasiswaLoc =  LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mahasiswaLoc != null) {
                showDialogForFragment(getActivity());
                ngajarLoc.getLocation("lokasi", new LocationCallback() {
                    @Override
                    public void onLocationResult(String key, GeoLocation location) {
                        Location dosenLoc = new Location("dosen");
                        dosenLoc.setLatitude(location.latitude);
                        dosenLoc.setLongitude(location.longitude);


                        if (mahasiswaLoc.distanceTo(dosenLoc) <= 20) {
                            hideDialogForFragment();
                            String uid  = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            MahasiswaPresent mp = new MahasiswaPresent(uid);
                            locRef.child("mahasiswa").child(uid).setValue(mp)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Snackbar.make(rootView,"Berhasil Ikut Kelas!",Snackbar.LENGTH_LONG).show();
                                                Intent i = new Intent(getActivity(), DetailNgajarActivity.class);
                                                i.putExtra(EXTRA_FROM_LISTNGAJAR, k);
                                                startActivity(i);
                                                getActivity().finish();
                                            }else{
                                                Snackbar.make(rootView,"Gagal ikut kelas",Snackbar.LENGTH_LONG).show();
                                            }
                                        }
                                    });


                        }else if(mahasiswaLoc.distanceTo(dosenLoc) > 20){
                            hideDialogForFragment();
                            Snackbar.make(rootView,"Jarak terlalu jauh dari pengajar!",Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        hideDialogForFragment();
                        Log.e(TAG, databaseError.getMessage());
                    }
                });
            }else{
                Toast.makeText(getActivity(), "Lokasi tidak ada/error", Toast.LENGTH_SHORT).show();
            }
        }else{
            EasyPermissions.requestPermissions(getActivity(),"Minta Ijin",RC_PERMS_FINELOC,perms);
        }
    }


    public void showDialogForFragment(Context context){

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage("sedang diproses");
        }

        mProgressDialog.show();
    }


    public void hideDialogForFragment(){
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        ikutMatkul(key);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }
}
