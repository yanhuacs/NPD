package com.example.derekwu.substringofnull;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo(false, true);
        foo(false, false);
        foo(true, false);
    }

    public void foo(boolean b, boolean c) {
        String s = "abc";
        if (b) {
            s = null;
        }
        if (c) {
            s = s.substring(1);
        }
        String test = "com.example.derekwu.substringofnull."+s;
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
class bc{public void bc(){}}