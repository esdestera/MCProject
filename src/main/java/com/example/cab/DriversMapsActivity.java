package com.example.cab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.firebase.geofire.GeoFire;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class DriversMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiCLient;
    Location lastLocation;
    LocationRequest locationRequest;

    private Button logoutButton;
    private Button settingsButton;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    private Boolean currentLogoutStatus = false;

    LocationManager locationManager;

    private DatabaseReference assignedCustomerReference;
    private String driverId;
    private String customerId = "";
    private DatabaseReference assignedCustomerPickupReference;
    private Marker pickupMarker;

    private ValueEventListener assignedCustomerPickupReferenceListener;

    private TextView customerName, customerPhone;
    private CircleImageView customerProfilePhoto;
    private RelativeLayout relativeLayout;
    private Button accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_maps);

        logoutButton = findViewById(R.id.driver_logout);
        settingsButton = findViewById(R.id.driver_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverId = mAuth.getCurrentUser().getUid();

        customerName = findViewById(R.id.customer_name);
        relativeLayout = findViewById(R.id.rell);

        customerProfilePhoto = findViewById(R.id.customer_profle_image);
        accelerometer = findViewById(R.id.driver_accelerometer);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriversMapsActivity.this, SettingsMainActivity.class);
                intent.putExtra("type", "Drivers");
                startActivity(intent);
            }
        });


        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentLogoutStatus = true;
                DisconnectTheDriver();
                mAuth.signOut();
                LogoutDriver();
            }
        });

        accelerometer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriversMapsActivity.this, AccelerometerActivity.class);
                startActivity(intent);
            }
        });

        if(customerId != "")
        {
            GetAssignedCustomerRequest();
        }
    }

    private void GetAssignedCustomerRequest() {
        assignedCustomerReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId);
        assignedCustomerReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    customerId = dataSnapshot.getValue().toString();

                    GetAssignedCustomerPickUpLocation();
                    relativeLayout.setVisibility(View.VISIBLE);
                    GetCustomerInfrmation();
                }
                else{
                    relativeLayout.setVisibility(View.INVISIBLE);

                    customerId = "";
                    if(pickupMarker != null){
                        pickupMarker.remove();
                    }

                    if(assignedCustomerPickupReference != null){
                        assignedCustomerPickupReference.removeEventListener(assignedCustomerPickupReferenceListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void GetAssignedCustomerPickUpLocation() {
        assignedCustomerPickupReference = FirebaseDatabase.getInstance().getReference().child("Customers Request").child(customerId).child("l");
        assignedCustomerPickupReferenceListener = assignedCustomerPickupReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    List<Object> customerLocationMap = (List<Object>)dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if(customerLocationMap.get(0) != null){
                        locationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }

                    if(customerLocationMap.get(1) != null){
                        locationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Customer location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiCLient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        if(getApplicationContext() != null && FirebaseAuth.getInstance().getCurrentUser() != null){
            lastLocation = location;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers available");

            GeoFire geoFireAvailability = new GeoFire(driverAvailabilityRef);

            DatabaseReference driversWorkingReference = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            GeoFire geoFireWorking = new GeoFire(driversWorkingReference);

            switch (customerId){
                case "":
                    geoFireWorking.removeLocation(userId);
                    geoFireAvailability.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    geoFireAvailability.removeLocation(userId);
                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

            }
        }

    }

    protected synchronized void buildGoogleApiClient(){
        googleApiCLient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        googleApiCLient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(!currentLogoutStatus){
            DisconnectTheDriver();
        }
    }

    private void DisconnectTheDriver() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers available");

        GeoFire geoFire = new GeoFire(driverAvailabilityRef);
        geoFire.removeLocation(userId);

        DatabaseReference driverWorkers = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        GeoFire geoFire1 = new GeoFire(driverWorkers);
        geoFire1.removeLocation(userId);
    }

    private void LogoutDriver() {
        Intent welcomeIntent = new Intent(DriversMapsActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    private void GetCustomerInfrmation(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    String retrivedName = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();

                    customerName.setText(retrivedName);
                    customerPhone.setText(phone);
                    String image = dataSnapshot.child("image").getValue().toString();
                    Picasso.get().load(image).into(customerProfilePhoto);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
