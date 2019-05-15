package com.example.derekwu.fieldconditional;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private boolean b;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo();
    }

    public void foo() {
        b = false;
        String s = "ab";
        if (b) {
            s += "c";
        } else {
            s += "d";
        }
        String test = "com.example.derekwu.fieldconditional."+s;
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

class abd{public void abd(){}}
class abc{public void abc(){}}