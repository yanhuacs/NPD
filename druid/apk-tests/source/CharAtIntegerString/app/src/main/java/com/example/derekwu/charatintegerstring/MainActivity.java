package com.example.derekwu.charatintegerstring;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bar(-10);
        bar(5);
        bar(10);
        bar(11);
        bar(12);
        bar(13);
        bar(14);
        bar(15);
        bar(16);
        bar(17);
        bar(18);
        bar(19);
    }

    private String secondDigit(int x) {
        String s = "" + x;
        char ch;
        if (s.length() < 2)
            ch = 'A';
        else
            ch = s.charAt(1);
        return "" + ch;
    }
    private void bar(int x) {
        String test="com.example.derekwu.charatintegerstring.";
        String s ="x"+secondDigit(x);
        try {
            Class<?> clz=Class.forName(test+s);
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
class x0{public void x0(){}}
class x1{public void x1(){}}
class x2{public void x2(){}}
class x3{public void x3(){}}
class x4{public void x4(){}}
class x5{public void x5(){}}
class x6{public void x6(){}}
class x7{public void x7(){}}
class x8{public void x8(){}}
class x9{public void x9(){}}
class xA{public void xA(){}}