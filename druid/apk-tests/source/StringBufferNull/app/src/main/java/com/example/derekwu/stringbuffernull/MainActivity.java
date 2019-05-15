package com.example.derekwu.stringbuffernull;

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

    public void foo(boolean bool) {
        StringBuffer b;
        if (bool) {
            b = null;
        } else {
            b = new StringBuffer();
        }
        String s = "abc" + b;
        String test = "com.example.derekwu.stringbuffernull."+s;
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

class abcnull{public void abcnull(){}}
