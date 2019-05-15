package com.example.derekwu.charcast;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo("abcd", 65, 3);
        foo("xyz", 65, 8);
    }

    private void foo(String s, int x, int y) {
        char a = (char)x;
        char b = (char)(x + y);
        String test = "com.example.derekwu.charcast."+s+a+b;
        try {
            Class<?> clz=Class.forName(test);
            Object o = clz.newInstance();
            Method method=clz.getMethod(s+a+b);
            Method dMethod=clz.getDeclaredMethod(s+a+b);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        };
    }
}

class abcdAD{public void abcdAD(){}}
class xyzAI{public void xyzAI(){}}
