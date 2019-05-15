package com.example.derekwu.fieldcallmethod;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private String field;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo();
    }

    public void foo() {
        field = "foo";
        bar();
        String s=field;
        String test = "com.example.derekwu.fieldcallmethod."+s;
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

    private String bar() {
        baz();
        return field;
    }

    private void baz() {
        field = "baz";
    }
}

class baz{public void baz(){}}
class foo{public void foo(){}}