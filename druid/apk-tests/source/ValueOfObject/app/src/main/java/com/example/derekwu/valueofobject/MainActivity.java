package com.example.derekwu.valueofobject;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo(true);
        foo(false);
    }

    public void foo(boolean b){
        String x = "abc";
        String y = "xyz";
        Object o1;
        if (b) {
            o1 = x;
        } else {
            o1 = y;
        }
        String s = String.valueOf(o1);
        String test = "com.example.derekwu.valueofobject."+s;
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
class xyz{public void xyz(){}}