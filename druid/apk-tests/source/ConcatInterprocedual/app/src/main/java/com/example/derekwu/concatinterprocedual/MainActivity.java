package com.example.derekwu.concatinterprocedual;


import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String a="a";
        String b=addA(a);
        String s= "stringanalysis.string.testcase."+b;
        try {
            Class<?> clz=Class.forName(s);
            Object o = clz.newInstance();
            Method method=clz.getMethod(b);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String addA(String v)
    {
        return v+"A";
    }
}
class aA{public void aA(){System.out.println("aA");}}
