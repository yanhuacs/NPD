package com.example.derekwu.replacesubwithsuper;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo();
    }

    public void foo() {
        String s = "abcbc";
        s = s.replace("bc", "XbcY");
        String test = "com.example.derekwu.replacesubwithsuper."+s;
        try {
            Class<?> clz=Class.forName(test);
            Object o = clz.newInstance();
            Method method=clz.getMethod(s);
            Method dMethod=clz.getDeclaredMethod(s);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        };
    }
}

class aXbcYXbcY{public void aXbcYXbcY(){}}
