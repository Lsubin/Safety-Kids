package com.example.daycarecenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    EditText id;
    EditText pw;
    Button login;

    String idText;
    String pwText;
    String phoneNum;

    Boolean status;
    int count;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();

    private FirebaseAuth dbAuth;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbAuth = FirebaseAuth.getInstance();

        id = (EditText)findViewById(R.id.id);
        pw = (EditText)findViewById(R.id.password);
        login = (Button)findViewById(R.id.login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                idText = id.getText().toString().trim();
                pwText =  pw.getText().toString().trim();

                //회원가입 할 때 적은 이메일과 비밀번호를 파이어베이스에서 일치하는지 알아서 확인 후 로그인 시킴
                dbAuth.signInWithEmailAndPassword(idText, pwText).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                            //이메일 확인
                            {
                                checkEmail();
                            }

                        }
                        else
                            Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void checkEmail() {
        count = 1;
        status = false;
        myDB.child("Teacher").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount() != 0){
                    for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        //선생님 이메일이면 선생님 엑티비티로 이동
                        if(idText.equals(postSnapShot.child("email").getValue().toString()))
                        {
                            status = true;
                            Intent intent = new Intent(getApplicationContext(), TeacherActivity.class);
                            intent.putExtra("TeacherName",idText.substring(0,3));
                            startActivity(intent);
                            finish();
                            break;
                        }
                        //부모님 이메일이면 부모님 엑티비티로 이동
                        else if(count == dataSnapshot.getChildrenCount() && status == false)
                        {
                            Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
                            intent.putExtra("ParentEmail",idText);
                            intent.putExtra("ParentName",idText.substring(0,3));
                            startActivity(intent);
                            finish();
                        }
                        count++;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void GotoJoin(View v){
        Button goJoin = (Button)findViewById(R.id.goJoin);

        makeDialog();
    }

    private void makeDialog() {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
        alt_bld.setTitle("선택").setCancelable(false).setPositiveButton("학부모", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Log.v("알림", "다이얼로그 > 학부모 선택");
                GoJoinParentActivity();
            }
        }).setNeutralButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v("알림", "다이얼로그 >취소");

            }
        }).setNegativeButton("선생님 ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.v("알림", "다이얼로그 > 선생님 선택");
                GoJoinActivity();
            }
        });
        AlertDialog alert = alt_bld.create();
        alert.show();
    }

    private void GoJoinParentActivity() {
        Intent intent = new Intent(this, JoinParentActivity.class);
        startActivity(intent);
    }

    public void GoJoinActivity() {
        Intent intent = new Intent(this, JoinActivity.class);
        startActivity(intent);
    }

}