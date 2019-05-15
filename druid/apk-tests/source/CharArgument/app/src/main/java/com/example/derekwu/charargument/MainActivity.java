package com.example.derekwu.charargument;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo('A');
        foo('B');
    }

    private void foo(char ch){
        String s = "x";
        s += ch;
        s += ch;
        String x="com.example.derekwu.charargument."+s;
        try {
            Class<?> clz=Class.forName(x);
            Object o = clz.newInstance();
            Method method=clz.getMethod(s);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

class xAA{
    public void xAA(){
        System.out.println("A");
    }
}

class xBB{
    public void xBB(){
        System.out.println("B");
    }
}
