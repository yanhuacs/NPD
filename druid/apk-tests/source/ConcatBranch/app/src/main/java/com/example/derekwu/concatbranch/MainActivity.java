package com.example.derekwu.concatbranch;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String a="a";
        String b="b";
        String c="c";
        String e="e";
        long time=(int)(Math.random()*100);
        if(time % 2 ==0)
        {
            a+=a+"a";
            b+=a+b;
        }
        else{
            a+=a+"t";
            b+=a+b;
        }
        c+=b+c;

        String s= "com.example.derekwu.concatbranch."+c;
        try {
            Class<?> clz=Class.forName(s);
            Object o = clz.newInstance();
            Method method=clz.getMethod(c);
            Method dMethod=clz.getDeclaredMethod(c);
            method.invoke(o);
        } catch (Exception g) {
            // TODO Auto-generated catch block
            g.printStackTrace();
        };

        a="a";
        b="b";
        c="c";
        e="e";
        time=(int)(Math.random()*100);
        if(time % 2 ==0)
        {
            a+=a+"a";
            b+=a+b;
        }
        else{
            a+=a+"t";
            b+=a+b;
        }
        c+=b+c;

        s= "com.example.derekwu.concatbranch."+c;
        try {
            Class<?> clz=Class.forName(s);
            Object o = clz.newInstance();
            Method method=clz.getMethod(c);
            Method dMethod=clz.getDeclaredMethod(c);
            method.invoke(o);
        } catch (Exception g) {
            // TODO Auto-generated catch block
            g.printStackTrace();
        };
    }
}
class cbaatbc{public void cbaatbc(){}}
class cbaaabc{public void cbaaabc(){}}

