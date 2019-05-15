package com.example.derekwu.arrayassignment;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        test(2, 2);
        test(1, 2);
    }

    public static void test(int i, int j) {
        String[] array = new String[5];

        array[i] = "foo";
        array[j] = "bar";

        String s = "com.example.derekwu.arrayassignment."+array[i];
        try {
            Class<?> clz=Class.forName(s);
            Object o = clz.newInstance();
            Method method=clz.getMethod(array[i]);
            Method dMethod=clz.getDeclaredMethod(array[i]);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        };
    }
}
class foo{public void foo(){}}
class bar{public void bar(){}}