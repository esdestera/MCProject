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

public class CustomerLoginRegisterActivity extends AppCompatActivity {

    private Button customerLoginButton;
    private Button customerRegisterButton;
    private Button customerRegisterLink;
    private TextView customerStatus;
    private EditText customerEmail;
    private EditText customerPassword;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference customerDatabaseReference;
    private String onlineCustomerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!isConnected(CustomerLoginRegisterActivity.this)) {
            Intent intent = new Intent(CustomerLoginRegisterActivity.this, WelcomeActivity.class);
            showNotification(CustomerLoginRegisterActivity.this, "Wifi connection", "Please check wifi connection", intent);
        }
        else {

            setContentView(R.layout.activity_customer_login_register);

            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            loadingBar = new ProgressDialog(this);
            if (mAuth.getCurrentUser() != null) {
                onlineCustomerId = mAuth.getCurrentUser().getUid();
                customerDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(onlineCustomerId);
            }


            customerLoginButton = findViewById(R.id.customer_login_button);
            customerRegisterButton = findViewById(R.id.customer_register_button);
            customerRegisterLink = findViewById(R.id.register_customer_link);
            customerStatus = findViewById(R.id.driver_status);
            customerEmail = findViewById(R.id.customer_email);
            customerPassword = findViewById(R.id.customer_password);

            customerRegisterButton.setVisibility(View.INVISIBLE);
            customerStatus.setText("Login customer");

            customerRegisterLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    customerLoginButton.setVisibility(View.INVISIBLE);
                    customerRegisterButton.setVisibility(View.VISIBLE);
                    customerRegisterLink.setVisibility(View.INVISIBLE);
                    customerStatus.setText("Register customer");
                }
            });

            customerRegisterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = customerEmail.getText().toString();
                    String password = customerPassword.getText().toString();

                    RegisterCustomer(email, password);
                }
            });

            customerLoginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String email = customerEmail.getText().toString();
                    String password = customerPassword.getText().toString();

                    SignInCustomer(email, password);
                }
            });
        }
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


    private void SignInCustomer(String email, String password) {
        if(TextUtils.isEmpty(email)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please write email...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please write password...", Toast.LENGTH_SHORT).show();
        }

        else{
            loadingBar.setTitle("Customer sign in.");
            loadingBar.setMessage("Please wait while we checking your credentials.");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer signed in successfully", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                        Intent customerIntent = new Intent(CustomerLoginRegisterActivity.this, CustomersMapsActivity.class);
                        startActivity(customerIntent);
                    }
                    else{

                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer signing in unsuccessful. Please try again.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }

                }
            });
        }
    }

    private void RegisterCustomer(String email, String password){
        if(TextUtils.isEmpty(email)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please write email...", Toast.LENGTH_SHORT).show();
        }

        if(TextUtils.isEmpty(password)){
            Toast.makeText(CustomerLoginRegisterActivity.this, "Please write password...", Toast.LENGTH_SHORT).show();
        }

        else{
            loadingBar.setTitle("Customer registration.");
            loadingBar.setMessage("Please wait while we are register your data.");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        onlineCustomerId = mAuth.getCurrentUser().getUid();
                        customerDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(onlineCustomerId);

                        customerDatabaseReference.setValue(true);

                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer register successfully", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();

                        Intent driverIntent = new Intent(CustomerLoginRegisterActivity.this, CustomersMapsActivity.class);
                        startActivity(driverIntent);
                    }
                    else{

                        Toast.makeText(CustomerLoginRegisterActivity.this, "Customer registration unsuccessful. Please try again.", Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }

                }
            });
        }
    }
}
