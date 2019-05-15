package com.example.derekwu.iflengthequalsconst;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo("abc");
        foo("abcdef");
        foo("abcdefabc");
        String s = "x";
        for (int i=0; i<6; i++) {
            s += "y";
            foo(s);
        }
    }

    private static void foo(String s) {
        if (s.length() == 6) {

            String test = "com.example.derekwu.iflengthequalsconst."+s;
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
}
class abcdef{public void abcdef(){}}
class xyyyyy{public void xyyyyy(){}}
class abc{public void abc(){}}
