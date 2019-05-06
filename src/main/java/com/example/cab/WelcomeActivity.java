package com.example.cab;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {
private Button WelcomeDriverButton;
    private Button WelcomeCustomerButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        WelcomeCustomerButton = findViewById(R.id.WelcomeCustomer);
        WelcomeDriverButton = findViewById(R.id.WelcomeDriver);


        WelcomeCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isConnected(WelcomeActivity.this)) {
                    Intent intent = new Intent(WelcomeActivity.this, WelcomeActivity.class);
                    showNotification(WelcomeActivity.this, "Wifi connection", "Please check wifi connection", intent);
                }
                else {
                    Intent LoginRegisterCustomerIntent = new Intent(WelcomeActivity.this, CustomerLoginRegisterActivity.class);
                    startActivity(LoginRegisterCustomerIntent);
                }
            }
        });

        WelcomeDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isConnected(WelcomeActivity.this)) {
                    Intent intent = new Intent(WelcomeActivity.this, WelcomeActivity.class);
                    showNotification(WelcomeActivity.this, "Wifi connection", "Please check wifi connection", intent);
                }
                else {
                    Intent LoginRegisterDriverIntent = new Intent(WelcomeActivity.this, DriverLoginRegisterActivity.class);
                    startActivity(LoginRegisterDriverIntent);
                }
            }
        });
    }

    private void AddNotification() {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        notification.setSmallIcon(R.drawable.wifi);
        notification.setContentTitle("Wifi connection");
        notification.setContentText("Please check wifi connection");

        Intent intent = new Intent(this, CustomersMapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(pendingIntent);

        NotificationManager nm = (NotificationManager)getSystemService((NOTIFICATION_SERVICE));
        nm.notify(0, notification.build());

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
}
