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


        // 파이어베이스 스토리지 저장소 연결
        storage = FirebaseStorage.getInstance();

        profile_image = findViewById(R.id.profile_image);
        cameraBTN = findViewById(R.id.cameraBTN);
        btn_list = findViewById(R.id.btn_list);
        name = findViewById(R.id.name);
        startService = findViewById(R.id.startService);
        stopService = findViewById(R.id.stopService);
        btn_chat = findViewById(R.id.btn_chat);

        final Intent intent = getIntent();

        //로그인 화면에서 보낸 부모님 이메일 값 받기
        parentEmail = intent.getExtras().getString("ParentEmail");
        parentName = intent.getExtras().getString("ParentName");

        //탐색시작 버튼을 누르면 비콘 거리재기(백그라운드) 서비스 시작
        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serviceIntent = new Intent(getApplicationContext(), BackgroundService2.class);
                startService(serviceIntent);
                startService.setEnabled(false);
            }
        });
        //탐색종료 버튼을 누르면 비콘 거리재기 종료료
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

        //출석부 화면으로 이동
        btn_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ListActivity.class);
                startActivity(intent);
            }
        });

        // 알림 화면으로 이동
        btn_chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ChattingParentActivity.class);
                intent.putExtra("ParentName",parentName);
                startActivity(intent);
            }
        });

        //부모님의 아이 이름과 부모님이 설정한 프로필 사진 업데이트하는 함수
        getProfileInfo();

        checkPermission();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "뒤로가기 버튼 막음(로그인 방지)", Toast.LENGTH_SHORT).show();
        //super.onBackPressed();
    }

    // 위치 권한 주기
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},101);
        }
    }


    //사진, 이름 로드
    private void getProfileInfo() {
        joinDatabase.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount() != 0){
                    for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        //로그인한 부모님 이메일과 부모님 데이터베이스에 있는 이메일이 맞으면 사진 데베에 저장
                        if(parentEmail.equals(postSnapShot.child("ParentEmail").getValue().toString())) {
                            String kidName = postSnapShot.getKey();
                            //아이 이름으로 프로필 이름 변경
                            name.setText(kidName);
                            //부모님이 설정한 사진이 있으면
                            if (postSnapShot.getChildrenCount() > 2) {
                                saveURI = postSnapShot.child("FileURL").getValue().toString();
                                Picasso.get()
                                        .load(saveURI)
                                        .into(profile_image, new Callback.EmptyCallback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d("알림", "SUCCESS");
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

    // 권한 검사하는 함수
    public void checkPM() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
            makeDialog();
        } else {
            makeDialog();
        }
    }

    // 사진 설정하는 Dialog 띄워주는 함수
    private void makeDialog() {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(ProfileActivity.this);
        alt_bld.setTitle("프로필 사진 설정").setCancelable(false).setPositiveButton("사진촬영", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Log.v("알림", "다이얼로그 > 사진촬영 선택");
                flag = 0;
                takePhoto();
            }
        }).setNeutralButton("앨범선택", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v("알림", "다이얼로그 > 앨범선택 선택");
                flag = 1;
                selectAlbum();
            }
        }).setNegativeButton("취소 ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v("알림", "다이얼로그 > 취소 선택");
                dialog.cancel();
            }
        });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    // 앨범에서 이미지를 가져오는 함수
    public void selectAlbum(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setType("image/*");
        startActivityForResult(intent, FROM_ALBUM);
    }

    // 이미지를 직접 촬영하여 가져오는 함수
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
            Log.v("알림", "저장공간에 접근 불가능");
            return;
        }
    }

    public File createImageFile() throws IOException {
        String imgFileName = parentEmail + ".jpg";
        File imageFile = null;
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/Pictures", "DaycareCenter");
        if (!storageDir.exists()) {
            Log.v("알림", "storageDir 존재 x " + storageDir.toString());
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
                //앨범에서 가져오기
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
                //촬영
                try{
                    Log.v("알림", "FROM_CAMERA 처리");
                    galleryAddPic();
                    //이미지뷰에 이미지셋팅
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
        progressDialog.setMessage("프로필 사진을 저장중입니다.");
        progressDialog.show();

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.v("알림", "사진 업로드 실패");
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
        Toast.makeText(getApplicationContext(),"사진이 저장되었습니다",Toast.LENGTH_SHORT).show();

    }
    public void getURI(){
        StorageReference daycareCenter = storage.getReferenceFromUrl("gs://daycarecenter-6d0bc.appspot.com").child("userProfile").child(parentEmail);
        daycareCenter.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                profileURI = String.valueOf(uri);
                //부모님 데이터 베이스에 사진 넣기
                joinDatabase.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getChildrenCount() != 0){
                            for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                                //Toast.makeText(getApplicationContext(), postSnapShot.child("ParentEmail").getValue().toString(), Toast.LENGTH_SHORT).show();
                                //로그인한 부모님 이메일과 부모님 데이터베이스에 있는 이메일이 맞으면 사진 데베에 저장
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
