package com.example.derekwu.fieldsetonother;

import android.app.Activity;
import android.os.Bundle;

import java.lang.reflect.Method;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new FieldSetOnOther().foo(new FieldSetOnOther(), new FieldSetOnOther());
    }
}

class FieldSetOnOther {
    private String field;
    public void foo(FieldSetOnOther o1, FieldSetOnOther o2) {
        o1.field = "foo";
        bar(o1, o2);
        String s = "com.example.derekwu.fieldsetonother."+o1.field;
        try {
            Class<?> clz=Class.forName(s);
            Object o = clz.newInstance();
            Method method=clz.getMethod(o1.field);
            Method dMethod=clz.getDeclaredMethod(o1.field);
            method.invoke(o);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        };
    }

    private void bar(FieldSetOnOther o1, FieldSetOnOther o2) {
        o2.field = "bar";
    }

}

class foo{public void foo(){}}
class bar{public void bar(){}}
