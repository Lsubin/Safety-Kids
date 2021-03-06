package com.example.daycarecenter;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 10;
    private static final int CROP_FROM_IMAGE = 2;

    private FirebaseStorage storage;
    DatabaseReference joinDatabase = FirebaseDatabase.getInstance().getReference();
    String storagePermission[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    ImageView profile_image;
    ImageButton cameraBTN;
    Button btn_list;
    TextView name;
    Button startService;
    Button stopService;
    Button btn_chat;


    private Uri imgUri, photoURI;
    String profileURI;
    private String mCurrentPhotoPath;
    private static final int FROM_CAMERA = 0;
    private static final int FROM_ALBUM = 1;
    String saveURI;
    String parentEmail;
    String parentName;
    int flag;

    String state = "true";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();

    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        // ?????????????????? ???????????? ????????? ??????
        storage = FirebaseStorage.getInstance();

        profile_image = findViewById(R.id.profile_image);
        cameraBTN = findViewById(R.id.cameraBTN);
        btn_list = findViewById(R.id.btn_list);
        name = findViewById(R.id.name);
        startService = findViewById(R.id.startService);
        stopService = findViewById(R.id.stopService);
        btn_chat = findViewById(R.id.btn_chat);

        final Intent intent = getIntent();

        //????????? ???????????? ?????? ????????? ????????? ??? ??????
        parentEmail = intent.getExtras().getString("ParentEmail");
        parentName = intent.getExtras().getString("ParentName");

        //???????????? ????????? ????????? ?????? ????????????(???????????????) ????????? ??????
        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serviceIntent = new Intent(getApplicationContext(), BackgroundService2.class);
                startService(serviceIntent);
                startService.setEnabled(false);
            }
        });
        //???????????? ????????? ????????? ?????? ???????????? ?????????
        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(serviceIntent);
                startService.setEnabled(true);
            }
        });

       cameraBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPM();
            }
        });

        //????????? ???????????? ??????
        btn_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ListActivity.class);
                startActivity(intent);
            }
        });

        // ?????? ???????????? ??????
        btn_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ChattingParentActivity.class);
                intent.putExtra("ParentName",parentName);
                startActivity(intent);
            }
        });

        //???????????? ?????? ????????? ???????????? ????????? ????????? ?????? ?????????????????? ??????
        getProfileInfo();

        checkPermission();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "???????????? ?????? ??????(????????? ??????)", Toast.LENGTH_SHORT).show();
        //super.onBackPressed();
    }

    // ?????? ?????? ??????
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},101);
        }
    }


    //??????, ?????? ??????
    private void getProfileInfo() {
        joinDatabase.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount() != 0){
                    for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        //???????????? ????????? ???????????? ????????? ????????????????????? ?????? ???????????? ????????? ?????? ????????? ??????
                        if(parentEmail.equals(postSnapShot.child("ParentEmail").getValue().toString())) {
                            String kidName = postSnapShot.getKey();
                            //?????? ???????????? ????????? ?????? ??????
                            name.setText(kidName);
                            //???????????? ????????? ????????? ?????????
                            if (postSnapShot.getChildrenCount() > 2) {
                                saveURI = postSnapShot.child("FileURL").getValue().toString();
                                Picasso.get()
                                        .load(saveURI)
                                        .into(profile_image, new Callback.EmptyCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d("??????", "SUCCESS");
                                            }
                                        });
                                joinDatabase.removeEventListener(this);
                                break;
                            }
                        }
                        }
                    }
                }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    // ?????? ???????????? ??????
    public void checkPM() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
            makeDialog();
        } else {
            makeDialog();
        }
    }

    // ?????? ???????????? Dialog ???????????? ??????
    private void makeDialog() {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(ProfileActivity.this);
        alt_bld.setTitle("????????? ?????? ??????").setCancelable(false).setPositiveButton("????????????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Log.v("??????", "??????????????? > ???????????? ??????");
                flag = 0;
                takePhoto();
            }
        }).setNeutralButton("????????????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v("??????", "??????????????? > ???????????? ??????");
                flag = 1;
                selectAlbum();
            }
        }).setNegativeButton("?????? ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v("??????", "??????????????? > ?????? ??????");
                dialog.cancel();
            }
        });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    // ???????????? ???????????? ???????????? ??????
    public void selectAlbum(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setType("image/*");
        startActivityForResult(intent, FROM_ALBUM);
    }

    // ???????????? ?????? ???????????? ???????????? ??????
    public void takePhoto(){
        String state = Environment.getExternalStorageState();
        //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(Environment.MEDIA_MOUNTED.equals(state)){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(intent.resolveActivity(getApplicationContext().getPackageManager())!=null){
                File photoFile = null;
                try{
                    photoFile = createImageFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                if(photoFile!=null){
                    Uri providerURI = FileProvider.getUriForFile(getApplicationContext(),"com.example.daycarecenter", photoFile);
                    imgUri = providerURI;
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, providerURI);
                    startActivityForResult(intent, FROM_CAMERA);
                }
            }
        }else{
            Log.v("??????", "??????????????? ?????? ?????????");
            return;
        }
    }

    public File createImageFile() throws IOException {
        String imgFileName = parentEmail + ".jpg";
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "DaycareCenter");
        if (!storageDir.exists()) {
            Log.v("??????", "storageDir ?????? x " + storageDir.toString());
            storageDir.mkdirs();
        }
        imageFile = new File(storageDir, imgFileName);
        mCurrentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK){
            return;
        }
        switch (requestCode){
            case FROM_ALBUM : {
                //???????????? ????????????
                if(data.getData()!=null) {
                    try{
                        photoURI = data.getData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), photoURI);
                        profile_image.setImageBitmap(bitmap);
                        goToStorage();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                break;
            }
            case FROM_CAMERA : {
                //??????
                try{
                    Log.v("??????", "FROM_CAMERA ??????");
                    galleryAddPic();
                    //??????????????? ???????????????
                    profile_image.setImageURI(imgUri);
                    goToStorage();
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    public void goToStorage(){
        StorageReference daycareCenter = storage.getReferenceFromUrl("gs://daycarecenter-6d0bc.appspot.com").child("userProfile").child(parentEmail);
        UploadTask uploadTask;

        Uri file = null;
        if(flag == 0){
            file = Uri.fromFile(new File(mCurrentPhotoPath));
        }
        else if(flag == 1){
            file = photoURI;
        }
        uploadTask = daycareCenter.putFile(file);

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("????????? ????????? ??????????????????.");
        progressDialog.show();

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.v("??????", "?????? ????????? ??????");
                exception.printStackTrace();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                getURI();
                progressDialog.dismiss();
            }
        });
    }

    public void galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        getApplicationContext().sendBroadcast(mediaScanIntent);
        Toast.makeText(getApplicationContext(),"????????? ?????????????????????",Toast.LENGTH_SHORT).show();

    }
    public void getURI(){
        StorageReference daycareCenter = storage.getReferenceFromUrl("gs://daycarecenter-6d0bc.appspot.com").child("userProfile").child(parentEmail);
        daycareCenter.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                profileURI = String.valueOf(uri);
                //????????? ????????? ???????????? ?????? ??????
                joinDatabase.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getChildrenCount() != 0){
                            for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                                //Toast.makeText(getApplicationContext(), postSnapShot.child("ParentEmail").getValue().toString(), Toast.LENGTH_SHORT).show();
                                //???????????? ????????? ???????????? ????????? ????????????????????? ?????? ???????????? ????????? ?????? ????????? ??????
                                if(parentEmail.equals(postSnapShot.child("ParentEmail").getValue().toString()))
                                {
                                    String kidName = postSnapShot.getKey();
                                    //Toast.makeText(getApplicationContext(), kidName,Toast.LENGTH_SHORT).show();
                                    joinDatabase.child("Kid").child(kidName).child("FileURL").setValue(profileURI);
                                    break;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }
}
