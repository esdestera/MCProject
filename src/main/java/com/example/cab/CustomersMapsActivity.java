package com.example.cab;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.firebase.geofire.GeoFire;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiCLient;
    Location lastLocation;
    LocationRequest locationRequest;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    private Button logoutButton;

    private Button callACabButton;
    private Button settingsButton;
    private DatabaseReference databaseReference;

    private LatLng customerPickUpLocation;
    private DatabaseReference availableDriversLocationReference;


    private String customerId;
    private int radius;
    private boolean driverFound = false;
    private String driverFoundId;

    private DatabaseReference driverReference;
    private DatabaseReference driversLocationReference;

    private Marker driverMarker, pickupMarker;
    private Boolean requestType = false;
    private ValueEventListener driverLocationRefListener;
    private GeoQuery geoQuery;

    private TextView driverName, driverPhone, driverCarName;
    private CircleImageView driverProfilePhoto;
    private RelativeLayout relativeLayout;

    private final String CHANNEL_ID = "personal_notifications";
    NotificationCompat.Builder notification;
    private final int uniqueId = 121312;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_maps);

        logoutButton = findViewById(R.id.cusomer_logout);
        settingsButton = findViewById(R.id.customer_settings);
        callACabButton = findViewById(R.id.call_a_cab);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Customers Request");
        availableDriversLocationReference = FirebaseDatabase.getInstance().getReference().child("Drivers available");
        driversLocationReference = FirebaseDatabase.getInstance().getReference().child("Drivers Working");


        driverName = findViewById(R.id.driver_name);
        driverPhone = findViewById(R.id.driver_phone);
        driverCarName = findViewById(R.id.driver_car);
        relativeLayout = findViewById(R.id.rell);
        driverProfilePhoto = findViewById(R.id.driver_profle_image);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CustomersMapsActivity.this, SettingsMainActivity.class);
                intent.putExtra("type", "Customers");
                startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                LogoutCustomer();
            }
        });

        callACabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (requestType) {
                    requestType = false;
                    geoQuery.removeAllListeners();

                    driversLocationReference.removeEventListener(driverLocationRefListener);

                    if (driverFoundId != null) {
                        driverReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("CustomerRideId");
                        driverReference.removeValue();
                        driverFoundId = null;
                    }

                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(driverReference);
                    geoFire.removeLocation(customerId);

                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }

                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    callACabButton.setText("Call a cab");

                    relativeLayout.setVisibility(View.GONE);
                } else {

                    requestType = true;
                    customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    GeoFire geoFire = new GeoFire(databaseReference);
                    geoFire.setLocation(customerId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));

                    customerPickUpLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(customerPickUpLocation).title("My location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callACabButton.setText("Getting your driver");
                    GetClosestDriverCab();
                }
            }
        });
    }

    private void GetClosestDriverCab() {
        GeoFire geoFire = new GeoFire(availableDriversLocationReference);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPickUpLocation.latitude, customerPickUpLocation.longitude), radius);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound && requestType) {
                    driverFound = true;
                    driverFoundId = key;

                    driverReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideId", customerId);
                    driverReference.updateChildren(driverMap);

                    GettingDriverLocation();
                    callACabButton.setText("Looking for driver location...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius += 1;
                    GetClosestDriverCab();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GettingDriverLocation() {
        driverLocationRefListener = driversLocationReference.child(driverFoundId).child("l").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestType) {
                    List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    callACabButton.setText("Driver found");
                    relativeLayout.setVisibility(View.VISIBLE);
                    GetAssingedDriverInfrmation();

                    if (driverLocationMap.get(0) != null) {
                        locationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                    }

                    if (driverLocationMap.get(1) != null) {
                        locationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                    }

                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (driverMarker != null) {
                        driverMarker.remove();
                    }

                    Location driverCurrentLocation = new Location("");

                    driverCurrentLocation.setLatitude(driverLatLng.latitude);
                    driverCurrentLocation.setLongitude(driverLatLng.longitude);


                    Location currentCustomerLocation = new Location("");
                    currentCustomerLocation.setLongitude(customerPickUpLocation.longitude);
                    currentCustomerLocation.setLatitude(customerPickUpLocation.latitude);


                    float distance = currentCustomerLocation.distanceTo(driverCurrentLocation);


                    if (distance < 90) {
                        callACabButton.setText("Driver reached");
                    } else {
                        callACabButton.setText("Driver found: " + String.valueOf(distance));
                    }

                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your driver is here").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiCLient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        googleApiCLient.connect();
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
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    private void LogoutCustomer() {
        Intent welcomeIntent = new Intent(CustomersMapsActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    private void GetAssingedDriverInfrmation() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    String retrivedName = dataSnapshot.child("name").getValue().toString();
                    String phone = dataSnapshot.child("phone").getValue().toString();
                    String car = dataSnapshot.child("car").getValue().toString();

                    driverName.setText(retrivedName);
                    driverPhone.setText(phone);
                    driverCarName.setText(car);
                    String image = dataSnapshot.child("image").getValue().toString();
                    Picasso.get().load(image).into(driverProfilePhoto);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {


            }
        });
    }
}