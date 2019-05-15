package com.example.derekwu.irrelevantargs;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo("irrelevant1", "abc", "irrelevant2");
    }

    public void foo(String arg1, String s, String arg2) {
        String x = s;
        String test = "com.example.derekwu.irrelevantargs."+x;
        try {
            Class<?> clz=Class.forName(test);
            Object o = clz.newInstance();
            Method method=clz.getMethod(x);
            Method dMethod=clz.getDeclaredMethod(x);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        };
    }

}

class abc{public void abc(){}}