package com.example.derekwu.fieldnested;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private String apples;
    private String oranges;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo();
    }

    public void foo() {
        apples = oranges = "foo";
        bar();
        System.out.println(oranges);
        String s=apples+oranges;
        String test = "com.example.derekwu.fieldnested."+s;
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
    private void bar() {
        baz();
        bong();
    }
    private void baz() {
        apples = "baz";
    }
    private void bong() {
        oranges = apples;
    }
}
class bazbaz{public void bazbaz(){}}