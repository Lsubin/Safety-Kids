package com.example.daycarecenter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.minew.beacon.BeaconValueIndex;
import com.minew.beacon.BluetoothState;
import com.minew.beacon.MinewBeacon;
import com.minew.beacon.MinewBeaconManager;
import com.minew.beacon.MinewBeaconManagerListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

// 비콘이 쓰이는 클래스는 BeaconConsumer 인터페이스
public class AttendanceBookActivity extends AppCompatActivity {

    private CheckBox check;
    //private String name;
    private String beaconId;
    private String rssi;
    private TextView txtRssi;
    private TextView txtTime;
    private Button startService;
    private Button stopService;
    private double distanceAvg;
    private double resultDistance;

    private AttendanceBookAdapter adapter;
    private ListView listView;


    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_book);
        txtRssi = (TextView) findViewById(R.id.rssi);
        startService = (Button) findViewById(R.id.startService);
        stopService = (Button) findViewById(R.id.stopService);

        //리스트뷰와 연결
        listView = (ListView) findViewById(R.id.listBook);
        adapter = new AttendanceBookAdapter();
        listView.setAdapter(adapter);

        //리스트 뷰에 아이들 정보 넣기
        getKidInfo();

        //출석 버튼 누르면 비콘 거리 재기 시작
        startService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtRssi.setText("출석 시작");
                Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
                startService(intent);
                startService.setEnabled(false);
            }
        });

        //종료 보튼 누르면 비콘 거리 재기 종료
        stopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtRssi.setText("출석 종료");
                Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
                stopService(intent);
                startService.setEnabled(true);
            }
        });

    }

    //뒤로가기 버튼을 누르면 선생님 화면으로 나가면서 멈춰진 백그라운드 서비스 다시 실행
    @Override
    public void onBackPressed() {
        Toast.makeText(this, "비콘 거리재기 다시 시작", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
        startService(intent);
        super.onBackPressed();
    }


    //리스트 뷰에 학생 데이터 정보 넣기
    private void getKidInfo() {
        myDB.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() != 0) {
                    for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        String name = postSnapShot.getKey();
                        String beaconID = postSnapShot.child("BeaconID").getValue().toString();
                        String checked = postSnapShot.child("Checked").getValue().toString();
                        adapter.addItem(Boolean.parseBoolean(checked), name, "ID: " + beaconID);
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void check(double distance) {
        if(distance <= 3) {
            myDB.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //데이터베이스에서 check 값을 가져와서 체크박스 바꾸기
                    if (dataSnapshot.getChildrenCount() != 0) {
                        for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                            String id = postSnapShot.child("BeaconID").getValue().toString();
                            if(id.equals(beaconId)) {  //데베 있는 아이디랑 비콘 아이디가 같으면
                                String checked = postSnapShot.child("Checked").getValue().toString();
                                if (checked.equals("false")) {
                                    Toast.makeText(getApplicationContext(), "3미터 이내로 출석체크 완료", Toast.LENGTH_SHORT).show();
                                    String name = postSnapShot.getKey().toString();
                                    myDB.child("Kid").child(name).child("Checked").setValue("true");
                                    adapter.clear();  //리스트 뷰 삭제하고
                                    getKidInfo();  //다시 정보 가져옴
                                    adapter.notifyDataSetChanged();
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

        else {
            myDB.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //데이터베이스에서 check 값을 가져와서 체크박스 바꾸기
                    if (dataSnapshot.getChildrenCount() != 0) {
                        for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                            String id = postSnapShot.child("BeaconID").getValue().toString();
                            if(id.equals(beaconId)) {  //데베 있는 아이디랑 비콘 아이디가 같으면
                                String checked = postSnapShot.child("Checked").getValue().toString();
                                if (checked.equals("true")) {
                                    Toast.makeText(getApplicationContext(), "3미터 밖으로 하원 완료 입니다.", Toast.LENGTH_SHORT).show();
                                    String name = postSnapShot.getKey().toString();
                                    myDB.child("Kid").child(name).child("Checked").setValue("false");
                                    adapter.clear();  //리스트 뷰 삭제하고
                                    getKidInfo();  //다시 정보 가져옴
                                    adapter.notifyDataSetChanged();
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
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            resultDistance = intent.getDoubleExtra("distacne", 0);
            distanceAvg = intent.getDoubleExtra("avg", 0);
            beaconId = intent.getStringExtra("beaconID");
            Log.i("receiver", "받아온 거리 값:" + distanceAvg);
            txtRssi.setText("거리: " + String.format("%.3f", resultDistance));
            check(resultDistance);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("Get-avgDistance"));
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }


}
