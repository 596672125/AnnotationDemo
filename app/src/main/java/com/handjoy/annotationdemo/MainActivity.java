package com.handjoy.annotationdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.handjoy.demoannotation.BindView;
import com.handjoy.demoannotation.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.hello)
    TextView mTv;
    @BindView(R.id.test)
    Button mBtnTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivity_ViewBinding.inject(this);
        mTv.setText("not crash");
        mBtnTest.setText("btn");
    }


    @OnClick({R.id.hello,R.id.test})
    void onViewClick(View v){

    }


}
