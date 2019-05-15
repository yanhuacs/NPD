package com.example.derekwu.containvariable;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        foo("Hello world!", "world");
        foo("The world is round!", "wxs");
    }

    private void foo(String t, String y) {
        boolean b = t.contains(y);
        String s = "x" + b;
        String test = "com.example.derekwu.containvariable."+s;
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

class xtrue{public void xtrue(){}}
class xfalse{public void xfalse(){}}
