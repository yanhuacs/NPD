package com.example.derekwu.classwithtostring;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new ClassWithToString().foo(new ClassWithToString());
    }

}
class ClassWithToString {
    public static void foo(ClassWithToString str) {
        String s = "a" + str;
        String test = "com.example.derekwu.classwithtostring."+s;
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

    @Override
    public final String toString() {
        return "xx";
    }
}
class axx{public void axx(){}}