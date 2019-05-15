package com.example.derekwu.interfacemethod;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo(new InnerClass());
    }

    private interface Inner {
        String foo();
    }
    private static class InnerClass implements Inner {
        public String foo() {
            return "abc";
        }
    }
    public void foo(Inner inner) {
        String s = inner.foo();
        String test = "com.example.derekwu.interfacemethod."+s;
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

class abc{public void abc(){}}
