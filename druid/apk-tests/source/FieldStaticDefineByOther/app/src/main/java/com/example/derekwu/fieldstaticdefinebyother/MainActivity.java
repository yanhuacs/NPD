package com.example.derekwu.fieldstaticdefinebyother;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private static String field;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo();
    }

    public static void foo() {
        String s = "xyz";
        bar();
        s += field;
        String string = "com.example.derekwu.fieldstaticdefinebyother."+s;
        try {
            Class<?> clz=Class.forName(string);
            Object o = clz.newInstance();
            Method method=clz.getMethod(s);
            Method dMethod=clz.getDeclaredMethod(s);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        };
    }

    private static void bar() {
        field = "abc";
    }
}

class xyzabc{public void xyzabc(){}}
