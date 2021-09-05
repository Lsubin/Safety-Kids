package com.example.daycarecenter;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class JoinActivity extends AppCompatActivity {
    private FirebaseAuth dbAuth;
    private EditText makeEmail;
    private EditText makePassword;
    private EditText makeName;
    private EditText okPassword;
    private Button btnJoin;
    private Button btnCancel;

    int status;
    String id;
    String email;
    String name;
    String pw;
    String okpw;
    int count;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        makeEmail = (EditText) findViewById(R.id.makeEmail);
        makePassword = (EditText) findViewById(R.id.makePassword);
        makeName = (EditText) findViewById(R.id.makeName);
        okPassword = (EditText) findViewById(R.id.okPassword);
        btnJoin = (Button) findViewById(R.id.btnJoin);
        btnCancel = (Button) findViewById(R.id.btnCancel);

        //인증 선언
        dbAuth = FirebaseAuth.getInstance();

        //회원가입 버튼 누름
        btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = makeEmail.getText().toString().trim();
                name = makeName.getText().toString().trim();
                pw = makePassword.getText().toString().trim();
                okpw = okPassword.getText().toString().trim();

                //비밀번호 일치하는지 확인하고 일치하면 회원가입
               if (pw.equals(okpw)) {
                    checkName(email, name);
                } else if (!pw.equals(okpw))
                    Toast.makeText(getApplicationContext(), "비밀번호가 서로 다릅니다.", Toast.LENGTH_SHORT).show();

            }


        });

        //취소 버튼을 누르면 전 화면으로 돌아감
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    //데이터베이스에 들어가 있는 이메일, 이름 일치하는지 확인
    private void checkName(final String email, final String name) {
        count = 1;
        status = 0;
        //이름 확인
        myDB.child("Teacher").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount() != 0){
                    for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        //데이터베이스에 등록된 이름과 회원가입 창의 이름이 같으면 이메일 확인하는 함수로 넘어감
                        if(name.equals(postSnapShot.getKey())) {
                            status = 1004; //일치했을 때 값 0 -> 1004 변경
                            //데이터베이스 안에 일치하는 이름이 있을 경우 이메일 확인 함수로 넘어감
                            checkEmail(email, name);
                            break;
                        }
                        else if(count == dataSnapshot.getChildrenCount() && status == 0){ //파이어베이스 안에 일치하는 값이 없음
                            Toast.makeText(getApplicationContext(), "등록된 선생님 이름이 없습니다.", Toast.LENGTH_SHORT).show();
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

    //이메일 확인
    private void checkEmail(final String email, final String name) {
        myDB.child("Teacher").child(name).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getChildrenCount() != 0){
                    for(DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                        //데이터베이스에 있는 이름, 이메일과 회원가입 창의 이름, 이메일이 일치하면 회원가입
                        if(email.equals(postSnapShot.getValue())) {
                            creatUser(email,pw);
                            break;
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "등록된 선생님 이메일이 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void creatUser(final String email, final String pw) {
        dbAuth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(JoinActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(!task.isSuccessful()) {
                    try {
                        throw task.getException();
                    } catch(FirebaseAuthWeakPasswordException e) {
                        Toast.makeText(JoinActivity.this,"비밀번호는 6자리 이상이여야 합니다" ,Toast.LENGTH_SHORT).show();
                    } catch(FirebaseAuthInvalidCredentialsException e) {
                        Toast.makeText(JoinActivity.this,"email 형식에 맞지 않습니다." ,Toast.LENGTH_SHORT).show();
                    } catch(FirebaseAuthUserCollisionException e) {
                        Toast.makeText(JoinActivity.this,"이미 존재하는 email 입니다." ,Toast.LENGTH_SHORT).show();
                    } catch(Exception e) {
                        Toast.makeText(JoinActivity.this,"다시 확인해주세요.." ,Toast.LENGTH_SHORT).show();
                    }
                }else{

                    Toast.makeText(getApplicationContext(), "회원가입 성공", Toast.LENGTH_SHORT).show();
                    myDB.child("Teacher").child(name).child("password").setValue(pw);
                    //로그인 화면으로 이동
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.putExtra("name", name);
                    startActivity(intent);
                    finish();

                }
            }
        });
    }

}
