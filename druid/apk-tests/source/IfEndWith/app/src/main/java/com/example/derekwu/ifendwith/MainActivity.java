package com.example.derekwu.ifendwith;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo("bfooX");
        foo("fo");
        foo("ffoo");
    }

    private static void foo(String s) {
        if (s.endsWith("oo")) {
            String test = "com.example.derekwu.ifendwith."+s;
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
}
class ffoo{public void ffoo(){}}
class fo{public void fo(){}}