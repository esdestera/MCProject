package com.example.cab;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.CpuUsageInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsMainActivity extends AppCompatActivity {

private CircleImageView profileImageView;
        private EditText name, phoneNumber, carName;
        private ImageView closeButon, saveButton;
        private TextView profileChange;
    private String getType;
    private String checker = "";
    private Uri imageUri;
    private String myUri;
    private StorageTask uploadTask;
    private StorageReference storageProfilePicReference;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_main);

        getType = getIntent().getStringExtra("type");
        Toast.makeText(this, getType, Toast.LENGTH_SHORT).show();

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(getType);
        storageProfilePicReference = FirebaseStorage.getInstance().getReference().child("Profie pictures");


        profileImageView = findViewById(R.id.profile_image);
        name = findViewById(R.id.name);
        phoneNumber = findViewById(R.id.phone_number);
        carName = findViewById(R.id.driver_car_name);

        closeButon = findViewById(R.id.close_button);
        saveButton = findViewById(R.id.save_button);
        profileChange = findViewById(R.id.change_picture);

        closeButon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getType.equals("Drivers")){
                    startActivity(new Intent(SettingsMainActivity.this, DriversMapsActivity.class));
                }
                else{
                    startActivity(new Intent(SettingsMainActivity.this, CustomersMapsActivity.class));

                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checker.equals("clicked")){
                    ValidateControlers();
                }
                else{
                    ValidateAndSaveOnlyInormation();
                }
            }
        });

        profileChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checker = "clicked";

                CropImage.activity().
                        setAspectRatio(1, 1).
                        start(SettingsMainActivity.this);


            }
        });

    GetUserInformation();
    }

    private void ValidateAndSaveOnlyInormation() {
        if(TextUtils.isEmpty((name.getText().toString()))){
            Toast.makeText(this, "Please provide your name", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty((phoneNumber.getText().toString()))){
            Toast.makeText(this, "Please provide your phone number", Toast.LENGTH_SHORT).show();
        }
        else if(getType.equals("Drivers") && TextUtils.isEmpty((name.getText().toString()))){
            Toast.makeText(this, "Please provide your name", Toast.LENGTH_SHORT).show();
        }
        else{
            HashMap<String, Object> userMap = new HashMap<String, Object>();
            userMap.put("uid", mAuth.getCurrentUser().getUid());
            userMap.put("name", name.getText().toString());
            userMap.put("phone", phoneNumber.getText().toString());

            if(getType.equals("Drivers")){
                userMap.put("car", carName.getText().toString());

            }

            databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

            if(getType.equals("Drivers")){
                startActivity(new Intent(SettingsMainActivity.this, DriversMapsActivity.class));
            }
            else{
                startActivity(new Intent(SettingsMainActivity.this, CustomersMapsActivity.class));
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data!= null){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            profileImageView.setImageURI(imageUri);
        }
        else{

            if(getType.equals("Drivers")){
                startActivity(new Intent(SettingsMainActivity.this, DriversMapsActivity.class));
            }
            else{
                startActivity(new Intent(SettingsMainActivity.this, CustomersMapsActivity.class));

            }
            Toast.makeText(this, "Error. Try again!", Toast.LENGTH_SHORT).show();
        }
    }

    private void ValidateControlers(){
        if(TextUtils.isEmpty((name.getText().toString()))){
            Toast.makeText(this, "Please provide your name", Toast.LENGTH_SHORT).show();
        }

        else if(TextUtils.isEmpty((phoneNumber.getText().toString()))){
            Toast.makeText(this, "Please provide your phone number", Toast.LENGTH_SHORT).show();
        }
        else if(getType.equals("Drivers") && TextUtils.isEmpty((name.getText().toString()))){
            Toast.makeText(this, "Please provide your name", Toast.LENGTH_SHORT).show();
        }
        else if(checker.equals("clicked")){
            UploadProfilePicture();
        }
    }

    private void UploadProfilePicture() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Settings account information");
        progressDialog.setMessage("Please wait while we are updating your profile settings");
        progressDialog.show();

        if(imageUri != null){
            final StorageReference fileRef = storageProfilePicReference.child(mAuth.getCurrentUser().getUid() + ".jpg");
            uploadTask = fileRef.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }

                    return  fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        Uri downloadUri = task.getResult();
                        myUri = downloadUri.toString();

                        HashMap<String, Object> userMap = new HashMap<String, Object>();
                        userMap.put("uid", mAuth.getCurrentUser().getUid());
                        userMap.put("name", name.getText().toString());
                        userMap.put("phone", phoneNumber.getText().toString());
                        userMap.put("image", myUri);

                        if(getType.equals("Drivers")){
                            userMap.put("car", carName.getText().toString());

                        }

                        databaseReference.child(mAuth.getCurrentUser().getUid()).updateChildren(userMap);

                        progressDialog.dismiss();
                        if(getType.equals("Drivers")){
                            startActivity(new Intent(SettingsMainActivity.this, DriversMapsActivity.class));
                        }
                        else{
                            startActivity(new Intent(SettingsMainActivity.this, CustomersMapsActivity.class));

                        }
                    }
                }
            });
        }

        else {
            Toast.makeText(this, "Image is not selected!", Toast.LENGTH_SHORT).show();

        }
    }

    private void GetUserInformation(){
        databaseReference.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot){
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){

                    if(dataSnapshot.child("name").getValue() != null){
                        String retrivedName = dataSnapshot.child("name").getValue().toString();
                        name.setText(retrivedName);


                    }
                    if(dataSnapshot.child("phone").getValue() != null){
                        String phone = dataSnapshot.child("phone").getValue().toString();
                        phoneNumber.setText(phone);


                    }


                    if(getType.equals("Drivers"))
                    {
                        if(dataSnapshot.child("car").getValue() != null){
                            String car = dataSnapshot.child("car").getValue().toString();
                            carName.setText(car);
                        }
                    }


                        if(dataSnapshot.child("image").getValue() != null){
                            String image = dataSnapshot.child("image").getValue().toString();
                            Picasso.get().load(image).into(profileImageView);
                        }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
