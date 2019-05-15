package com.example.derekwu.ifstartwith;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo("sir");
        foo("alot");
        foo("siren");
        foo("ffoo");
    }

    private static void foo(String s) {
        if ("sirsneezealot".startsWith(s)) {
            String test = "com.example.derekwu.ifstartwith."+s;
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
class sir{public void sir(){}}
class ffoo{public void ffoo(){}}