package com.example.daycarecenter;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.style.BulletSpan;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.minew.beacon.MinewBeaconManager;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BackgroundService2 extends Service implements BeaconConsumer {

    private String beaconId;
    private String rssi;
    int count = 0;
    double[] array = new double[20];
    double sum = 0;
    double avg = 0;
    double distance = 0;
    int check;

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1000;
    private MinewBeaconManager mMinewBeaconManager;
    private static final int REQUEST_ENABLE_BT = 2;
    private boolean isScanning;

    private BeaconManager beaconManager;
    //감지된 비콘들을 임시로 담을 리스트
    private List<Beacon> beaconList = new ArrayList<>();

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("테스트", "서비스의 onCreate");

        // 실제로 비콘을 탐지하기 위한 비콘매니저 객체를 초기화
        beaconManager = BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        // 비콘 탐지를 시작
        beaconManager.bind(this);

        FirebaseMessaging.getInstance().subscribeToTopic("news");
        FirebaseInstanceId.getInstance().getToken();
    }

    private void startBeacon() {
        handler.sendEmptyMessage(0);
        count = 0;
        avg = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("테스트", "비콘 시작");
        startBeacon();

        return super.onStartCommand(intent, flags, startId);
    }

    Handler handler = new Handler() {

        public void handleMessage(Message msg) {

            // 비콘의 아이디와 거리를 측정하여 textView에 넣기
            for(Beacon beacon : beaconList){
                beaconId = beacon.getId2().toString();
                int txPower = beacon.getTxPower();
                double rssi = beacon.getRssi();
                double distance = calculateDistance(txPower, rssi);
                int beaconID = Integer.parseInt(beaconId);
                //배열에 거리값 넣기
                if(beaconID == 40001){
                    if(count < 20 ) {
                        inputArray(distance, count);
                        count++;
                    }
                    else {
                        count  = 0;
                        break;
                    }
                }
            }
            // 자기 자신을 1초마다 호출
            handler.sendEmptyMessageDelayed(0, 1000);
        }
    };

    // 거리 계산 함수
    protected static double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine distance, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    private void inputArray(double distance, int count) {
        array[count] = Double.parseDouble(String.format("%.3f", distance));
        Log.i("distance ","배열" + count + "번째 값: " + array[count]);
        if (count == 19) {
            /*
            for (int i = 0; i < array.length; i++) {
                //로그 찍어서 배열 값 한번에 보여줌
                Log.i("측정된 거리 값", i + " 번째 값" + array[i]);
            }*/
            Arrays.sort(array);
            /*
        for(int i = 0; i < array.length; i++){
            //로그 찍어서 배열 정렬 값 한번에 보여줌
            Log.i("정렬 후",i + " 번째 값" + array[i]);
        }*/
            //정렬 된 배열을 평균 내기!
            avgDistance();
        }
    }

    private void avgDistance() {
        for(int i = 5; i < array.length-5; i++) {
            sum += array[i];
        }
        avg = sum / (array.length-10);
        distance = avg * 0.7;
        Log.i("평균 값", ": " + avg);
        Log.i("거리 값", ": " + distance);
        //MapParentActivity로 엑티비티로 평균 값 전달
        sendAvg(avg, distance);

        //아이가 20미터이내 일 때 노티피케이션 알림!
        notifyAlarmNear20m(distance);
        notifyAlarmNear10m(distance);
        notifyAlarmNear5m(distance);

        sum = 0;
    }

    private void notifyAlarmNear20m(double avg) {
        if(avg >= 10 && avg < 20){
            //알림 내용 설정
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"default");
            builder.setSmallIcon(R.drawable.main);
            builder.setDefaults(Notification.DEFAULT_SOUND);
            builder.setVibrate(new long[]{1000, 1000});
            builder.setContentTitle("알림!");
            builder.setContentText("아이가 20m이내에 있어요!");
            //알림 표시
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(new NotificationChannel(
                        "default","20알림 채널", NotificationManager.IMPORTANCE_DEFAULT));
                notificationManager.notify(1,builder.build());
            }
        }
    }

    private void notifyAlarmNear10m(double avg) {
        if(avg >= 5 && avg < 10){
            //알림 내용 설정
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"default");
            builder.setSmallIcon(R.drawable.main);
            builder.setDefaults(Notification.DEFAULT_SOUND);
            builder.setVibrate(new long[]{1000, 1000});
            builder.setContentTitle("알림!");
            builder.setContentText("아이가 10m이내에 있어요! 얼른 찾아주세요.");
            //알림 표시
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(new NotificationChannel(
                        "default","10알림 채널", NotificationManager.IMPORTANCE_DEFAULT));
                notificationManager.notify(1,builder.build());
            }
        }
    }
    private void notifyAlarmNear5m(double avg) {
        if (avg >= 0 && avg < 5) {
            //알림 내용 설정
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default");
            builder.setSmallIcon(R.drawable.main);
            builder.setDefaults(Notification.DEFAULT_SOUND);
            builder.setVibrate(new long[]{1000, 1000});
            builder.setContentTitle("알림!");
            builder.setContentText("아이가 5m이내에 있어요! 아이를 찾으시면 선생님께 연락해주세요.");
            //알림 표시
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.createNotificationChannel(new NotificationChannel(
                        "default", "5알림 채널", NotificationManager.IMPORTANCE_DEFAULT));
                notificationManager.notify(1, builder.build());
            }
        }
    }

    private void sendAvg(double avg, double distance) {
        Intent intent = new Intent("Get-avgDistance");
        intent.putExtra("avg",avg); //거리 평균 값 넘기기
        intent.putExtra("distacne", distance);  // 거리 값 넘기기
        intent.putExtra("beaconID", beaconId); //비콘 아이디 값 넘기기
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this);
        handler.removeMessages(0);  // 비콘 (거리재는) 핸들러 종료
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            // 비콘이 감지되면 해당 함수가 호출된다. Collection<Beacon> beacons에는 감지된 비콘의 리스트가,
            // region에는 비콘들에 대응하는 Region 객체가 들어온다.
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    beaconList.clear();
                    for (Beacon beacon : beacons) {
                        beaconList.add(beacon);
                    }
                }
            }
        });
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }
}
