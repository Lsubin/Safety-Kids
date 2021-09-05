package com.example.daycarecenter;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.hdodenhof.circleimageview.CircleImageView;

public class TeacherActivity extends AppCompatActivity {

    private ImageView checkList;
    private Intent intent;
    String[] phoneNum = {"01041984796","01030909645"};
    int check;
    String Teachername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);


        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myDB = database.getReference();

        // 로그인된 선생님 이름 받아오기
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if (extras.containsKey("TeacherName")) {
                Teachername = extras.getString("TeacherName");
            }
        }

        //선생님 화면으로 오면 비콘 거리재기(백그라운드) 서비스 시작
        intent = new Intent(getApplicationContext(), BackgroundService.class);
        startService(intent);

        //위치 권한 주기
        checkPermission();

        //메세지 권한 주기
        checkPM();

        checkList = (ImageView) findViewById(R.id.checklist);
        //출석부 버튼 누르면 출석부로 이동
        checkList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(getApplicationContext(), AttendanceBookActivity.class);
                startActivity(intent2);
                stopService(intent);
            }
        });
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

    // 메세지 권한 검사하는 함수
    public void checkPM() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS}, 0);
        }
        else {
        }
    }


    // 알림 채팅 화면으로 가기
    public void Gotonotification(View v){
        CircleImageView picnic = (CircleImageView) findViewById(R.id.picnic);
        Intent intent = new Intent(this, ChattingTeacherActivity.class);
        intent.putExtra("TeacherName",Teachername);
        startActivity(intent);
    }
}