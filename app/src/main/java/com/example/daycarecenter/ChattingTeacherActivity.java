package com.example.daycarecenter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Notification;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChattingTeacherActivity extends AppCompatActivity {

    private Button btnSelect;
    private EditText editContent;
    private String gpsContent;
    private Button btnEnter;
    private String content;
    private ListView listChatting;
    List<String> list = new ArrayList<>();
    private ArrayAdapter<String> arrayAdapter;
    private String TeacherName;
    String firstContent;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting_teacher);

        btnSelect = (Button)findViewById(R.id.btnSelect);
        editContent = (EditText)findViewById(R.id.editContent);
        btnEnter = (Button)findViewById(R.id.btnEnter);
        listChatting = (ListView) findViewById(R.id.listChatting);

        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        listChatting.setAdapter(arrayAdapter);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);



        // 맨 처음 내용 담기!
        firstContent = editContent.getText().toString();

        // 현재 위치 gps값 받아와서 EditText에 넣기, 선생님 이름 받기
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if (extras.containsKey("gpsContent")) {
                gpsContent= extras.getString("gpsContent");
                editContent.setText("현장학습 현재 위치는 '" + gpsContent + "' 입니다. ^^");
            }
        }

        final Intent intent = getIntent();
        TeacherName = intent.getExtras().getString("TeacherName");

        addDBChat();

        // 선택 버튼을 눌렀을 때 선택 다이얼로그 띄우기
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dlg = new AlertDialog.Builder(ChattingTeacherActivity.this);
                dlg.setTitle("선택하세요.");
                final String[] array = new String[] {"현장학습 위치 보내기","잃어버린 원생을 찾아주세요!","원생을 찾았습니다!"};
                dlg.setIcon(R.drawable.main);
                dlg.setItems(array, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editContent.setText(array[i]);
                        // 현장학습 위치 보내기를 누르게 된다면, 현장학습 구글 맵 지도 띄우기
                        if(i == 0)
                        {
                            Intent intent = new Intent(getApplicationContext(), NotificationActivity.class);
                            intent.putExtra("TeacherName",TeacherName);
                            startActivity(intent);
                            finish();
                        }
                    }
                });

                //버튼 클릭시 동작
                dlg.setPositiveButton("종료", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(ChattingTeacherActivity.this, "종료 버튼을 누르셨습니다.", Toast.LENGTH_SHORT);
                    }
                });
                dlg.show();
            }
        });

        // 보내기 버튼을 눌렀을 때 edittext에 적혀져 있는 값 string 변수 content에 넣기
        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 선택을 안 했을 시에는 보내기 불가!
                content = editContent.getText().toString();
                if(!content.equals(firstContent))
                {
                    myDB.child("Chat").child(String.valueOf(arrayAdapter.getCount())).setValue(TeacherName + " : " +content); //databaseReference를 이용해 데이터 푸쉬
                    editContent.setText("선택창에서 선택하세요."); //입력창 초기화
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"선택창에서 메세지 내용을 선택하세요.", Toast.LENGTH_SHORT).show();
                }

                // 아이를 찾았다는 메세지를 보내면 데베에서 아이의 상태(LostState == false) 로 바꾸기
                if(content.equals("원생을 찾았습니다!"))
                {
                    changeLostState();
                }
            }
        });

        myDB.child("Chat").addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatConversation(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // 리시트뷰에 아무것도 없다면 파이어베이스 chat이라는 db루트 추가
    private void addDBChat() {
        if(arrayAdapter.getCount() == 0) {

            // 알림 첫 채팅 설정, 파이어베이스에 chat 만들면서
            myDB.child("Chat").child("0").setValue(TeacherName + " : " + "안녕하세요. 현장학습 위치 알림, 원생을 잃어버렸을 시 알림 채팅입니다.");

            //myDB.child("Chat").setValue(0);
        }
    }

    private void changeLostState() {
        myDB.child("Kid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                    if("40001".equals(postSnapShot.child("BeaconID").getValue().toString())){
                        String kidName = postSnapShot.getKey();
                        String lostState = postSnapShot.child("LostState").getValue().toString();
                        // 아이의 상태를 잃어버림에서 찾음으로 바꿈 ture -> false
                        if (lostState.equals("true"))
                        {
                            myDB.child("Kid").child(kidName).child("LostState").setValue("false");

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // 데이터베이스에서 내용 변화 감지 후 리스트뷰에 올리기
    private void chatConversation(DataSnapshot dataSnapshot) {
        int index = Long.valueOf(dataSnapshot.getChildrenCount()).intValue();
        //Toast.makeText(getApplicationContext(), String.valueOf(index), 0).show();
        String chat_msg;

        if (index > arrayAdapter.getCount()) {
            for(int i = arrayAdapter.getCount(); i < index; i++) {
                if(dataSnapshot.hasChild(String.valueOf(i))){
                    chat_msg = dataSnapshot.child(String.valueOf(i)).getValue().toString();
                    arrayAdapter.add(chat_msg);
                }
            }
        }
        arrayAdapter.notifyDataSetChanged();
    }
}
