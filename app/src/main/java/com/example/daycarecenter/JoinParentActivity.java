package com.example.daycarecenter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class JoinParentActivity extends AppCompatActivity {

    private FirebaseAuth dbAuth;
    private EditText makeEmail;
    private EditText makeName;
    private EditText makePassword;
    private EditText okPassword;
    private Button btnJoin;
    private Button btnCancel;
    private EditText makePhoneNum;

    String email;
    String name;
    String pw;
    String okpw;
    String phone;

    int count;
    int status;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_parent);

        makeEmail = (EditText) findViewById(R.id.makeEmail);
        makeName = (EditText) findViewById(R.id.makeName);
        makePassword = (EditText) findViewById(R.id.makePassword);
        okPassword = (EditText) findViewById(R.id.okPassword);
        btnJoin = (Button) findViewById(R.id.btnJoin);
        btnCancel = (Button) findViewById(R.id.btnCancel);
        makePhoneNum = (EditText) findViewById(R.id.makePhoneNum);

        //인증 선언
        dbAuth = FirebaseAuth.getInstance();

        //회원가입 버튼을 눌렀을 때
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = makeEmail.getText().toString().trim();
                name = makeName.getText().toString().trim();
                pw = makePassword.getText().toString().trim();
                okpw = okPassword.getText().toString().trim();
                phone = makePhoneNum.getText().toString().trim();

                //비밀번호 일치하는지 확인하고 일치하면 회원가입 -> 원생 이름 정보 존재하는지 확인하는 함수로 넘어감
                if (pw.equals(okpw)) {
                    checkKidsName(name);
                } else if (!pw.equals(okpw))
                    Toast.makeText(getApplicationContext(), "비밀번호가 서로 다릅니다.", Toast.LENGTH_SHORT).show();

            }
        });

        //취소 버튼을 눌렀을 때(이전 화면으로 돌아감)
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    //원생 이름이 데이터베이스 내에 있는 이름과 일치하는지 확인
    private void checkKidsName(final String name) {
        count = 1;
        status = 0;
        //이름 확인
        myDB.child("Kid").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() != 0) {
                    for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        //데이터베이스에 등록된 아이의 이름과 부모님이 입력한 아이 이름이 일치면 회원가입 함수로 넘어감
                        if (name.equals(postSnapShot.getKey())) {
                            status = 1004; //일치했을 때 값 0 -> 1004 변경
                            createUser(email, name, phone);
                            break;
                        } else if (count == dataSnapshot.getChildrenCount() && status == 0) { //파이어베이스 안에 일치하는 값이 없음
                            Toast.makeText(getApplicationContext(), "등록된 아이 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                        count++; //count: 파이어베이스 안에 있는 데이터의 값의 갯수를 세기 위하여 존재 -> toast가 여러번 뜨는 것을 방지
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

        //회원가입 -> 파이어베이스에 비밀번호 등록!
        private void createUser (final String email,final String name, final String phone){
            dbAuth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(JoinParentActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(!task.isSuccessful()) {
                        try {
                            throw task.getException();
                        } catch(FirebaseAuthWeakPasswordException e) {
                            Toast.makeText(JoinParentActivity.this,"비밀번호는 6자리 이상이여야 합니다" ,Toast.LENGTH_SHORT).show();
                        } catch(FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(JoinParentActivity.this,"email 형식에 맞지 않습니다." ,Toast.LENGTH_SHORT).show();
                        } catch(FirebaseAuthUserCollisionException e) {
                            Toast.makeText(JoinParentActivity.this,"이미 존재하는 email 입니다." ,Toast.LENGTH_SHORT).show();
                        } catch(Exception e) {
                            Toast.makeText(JoinParentActivity.this,"다시 확인해주세요.." ,Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(), "회원가입 성공", Toast.LENGTH_SHORT).show();
                        myDB.child("Kid").child(name).child("ParentEmail").setValue(email);
                        myDB.child("Kid").child(name).child("ParentPassword").setValue(pw);
                        myDB.child("Kid").child(name).child("ParentPhoneNum").setValue(phone);
                        myDB.child("Kid").child(name).child("LostState").setValue(false);

                        //로그인 화면으로 이동
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
}
