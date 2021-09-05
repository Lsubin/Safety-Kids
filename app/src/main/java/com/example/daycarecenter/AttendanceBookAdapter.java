package com.example.daycarecenter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;

import androidx.annotation.NonNull;

public class AttendanceBookAdapter extends BaseAdapter {

    private ArrayList<KidItem> mItems = new ArrayList<>();

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myDB = database.getReference();

    private Boolean beChecked = false;

    CheckBox check;

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_attendance_book, parent, false);
        }

        check = (CheckBox) convertView.findViewById(R.id.checkbox);
        TextView kidName = (TextView) convertView.findViewById(R.id.txtKidName);
        TextView beaconID = (TextView) convertView.findViewById(R.id.txtBeaconID);

        KidItem myItem = (KidItem) getItem(position);

        check.setChecked(myItem.getCheckBox());
        check.setEnabled(false);  //체크박스 수정못하게 막음
        kidName.setText(myItem.getName());
        beaconID.setText(myItem.getId());

        return convertView;
    }

    public void addItem(Boolean check, String name, String id){
        KidItem mItem = new KidItem(check,name,id);

        mItem.setCheckBox(check);
        mItem.setName(name);
        mItem.setId(id);

        mItems.add(mItem);
    }
    public void clear(){
        mItems.clear();
    }
}
