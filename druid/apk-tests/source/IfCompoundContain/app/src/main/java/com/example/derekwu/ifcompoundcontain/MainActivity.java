package com.example.derekwu.ifcompoundcontain;


import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bar("X");
        bar("Y");
        bar("Xbc");
    }

    private static void foo(String s, boolean x) {
        boolean z;
        if (x) {
            z = s.equals("Y");
        } else {
            z = s.contains("X");
        }
        if (z) {
            String test = "com.example.derekwu.ifcompoundcontain."+s;
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
    private static void bar(String s) {
        foo(s,true);
        foo(s,false);
    }
}
class X{public void X(){}}
class Y{public void Y(){}}
class Xbc{public void Xbc(){}}