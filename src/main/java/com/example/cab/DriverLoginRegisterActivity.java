package com.example.cab;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginRegisterActivity extends AppCompatActivity {
    private Button driverLoginButton;
    private Button driverRegisterButton;
    private Button driverRegisterLink;
    private TextView driverStatus;
    private EditText driverEmail;
    private EditText driverPassword;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference driverDatabaseReference;
    private String onlineDriverId;

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         if(!isConnected(DriverLoginRegisterActivity.this)) {
             Intent intent = new Intent(DriverLoginRegisterActivity.this, WelcomeActivity.class);
             showNotification(DriverLoginRegisterActivity.this, "Wifi connection", "Please check wifi connection", intent);
         }
         else {

             setContentView(R.layout.activity_driver_login_register);
             FirebaseApp.initializeApp(this);
             mAuth = FirebaseAuth.getInstance();

             if (mAuth.getCurrentUser() != null) {
                 onlineDriverId = mAuth.getCurrentUser().getUid();
                 driverDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(onlineDriverId);

             }

             loadingBar = new ProgressDialog(this);
             driverLoginButton = findViewById(R.id.driver_login_button);
             driverRegisterButton = findViewById(R.id.driver_register_button);
             driverRegisterLink = findViewById(R.id.driver_register_link);
             driverStatus = findViewById(R.id.driver_status);
             driverPassword = findViewById(R.id.driver_password);
             driverEmail = findViewById(R.id.driver_email);

             driverRegisterButton.setVisibility(View.INVISIBLE);
             driverStatus.setText("Login driver");

             driverRegisterLink.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     driverLoginButton.setVisibility(View.INVISIBLE);
                     driverRegisterButton.setVisibility(View.VISIBLE);
                     driverRegisterLink.setVisibility(View.INVISIBLE);
                     driverStatus.setText("Register driver");
                 }
             });

             driverRegisterButton.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     String email = driverEmail.getText().toString();
                     String password = driverPassword.getText().toString();

                     RegisterDriver(email, password);
                 }
             });

             driverLoginButton.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                     String email = driverEmail.getText().toString();
                     String password = driverPassword.getText().toString();

                     SignInDriver(email, password);
                 }
             });
         }
    }

    public void showNotification(Context context, String title, String body, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.wifi)
                .setContentTitle(title)
                .setContentText(body).
                        setAutoCancel(true);;

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }


    public boolean isConnected(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if((mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting())) return true;
            else return false;
        } else
            return false;
    }

    public AlertDialog.Builder buildDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("No Internet Connection");
        builder.setMessage("Please connect to a network!");

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                finish();
            }
        });

        return builder;
    }

    private void SignInDriver(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please write email...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please write password...", Toast.LENGTH_SHORT).show();
        }

        else{
            loadingBar.setTitle("Driver sign in.");
            loadingBar.setMessage("Please wait while we are checking your credentials.");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){

                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver signed in successfully", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                        Intent driverIntent = new Intent(DriverLoginRegisterActivity.this, DriversMapsActivity.class);
                        startActivity(driverIntent);
                    }
                    else{

                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver signing in unsuccessful. Please try again.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }

                }
            });
        }
    }

    private void RegisterDriver(String email, String password){
        if(TextUtils.isEmpty(email)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please write email...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(DriverLoginRegisterActivity.this, "Please write password...", Toast.LENGTH_SHORT).show();
        }

        else{
            loadingBar.setTitle("Driver registration.");
            loadingBar.setMessage("Please wait while we are register your data.");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        driverDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(onlineDriverId);
                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver register successfully", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                        Intent driverIntent = new Intent(DriverLoginRegisterActivity.this, DriversMapsActivity.class);
                        startActivity(driverIntent);
                    }
                    else{

                        Toast.makeText(DriverLoginRegisterActivity.this, "Driver registration unsuccessful. Please try again.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }

                }
            });
        }
    }
}
