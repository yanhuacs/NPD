package com.example.derekwu.ifequalsnotstring;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo("abc", "abc");
        foo("bar", new Object());
        foo("xyz", new StringBuffer("xyz"));
    }

    private static void foo(String s, Object y) {
        if (s.equals(y)) {
            System.out.println(s);
            String test = "com.example.derekwu.ifequalsnotstring."+s;
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
class abc{public void abc(){}}
class bar{public void bar(){}}
class xyz{public void xyz(){}}