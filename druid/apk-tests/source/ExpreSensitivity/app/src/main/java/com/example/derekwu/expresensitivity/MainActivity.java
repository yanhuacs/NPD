package com.example.derekwu.expresensitivity;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String s="test";
        boolean b=savedInstanceState.getBoolean(s);
        foo(b);
    }

    public void foo(final boolean b) {
        String s = "x";
        if (b) {
            if (!b) {
                s = "y";
            }
        }
        // the usefulness of this test is arguable, since real code is almost always reachable
        String test = "com.example.derekwu.expresensitivity."+s;
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

class x{public void x(){}}
class y{public void y(){}}