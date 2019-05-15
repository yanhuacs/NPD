package com.example.derekwu.switchchar;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo('a');
        foo('b');
        foo('&');
    }

    private static void foo(char c) {
        String s = "";
        switch (c) {
            case '<':
            case '>':
            case '&':
                s += "t";
                break;
            default:
                s += c;
        }
        String test = "com.example.derekwu.switchchar."+s;
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
class a{public void a(){}}
class b{public void b(){}}
class t{public void t(){}}