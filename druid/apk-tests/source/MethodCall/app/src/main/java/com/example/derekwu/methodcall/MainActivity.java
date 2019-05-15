package com.example.derekwu.methodcall;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foo();
    }

    public void foo() {
        new GetX(); new GetZ();
        Foo foo = new GetY();
        String s = foo.foo();
        System.out.println(s);
        String test = "com.example.derekwu.methodcall."+s;
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

    public interface Foo {
        String foo();
    }
    public static class GetX implements Foo {	// implements interface directly
        public String foo() {
            return "x";
        }
    }
    public static class GetY implements Foo {	// implements, but has a subtype
        public String foo() {
            return "y";
        }
    }
    public static class GetZ extends GetY {		// subtype of GetY
        @Override
        public String foo() {
            return "z";
        }
    }
}
class y{public void y(){}}
class z{public void z(){}}