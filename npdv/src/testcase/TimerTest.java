package testcase;

import java.util.Timer;
import java.util.TimerTask;

public class TimerTest {

    public void test() {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

            }
        }, 200);

        dialog.setListener(new Listener() {
            @Override
            public void dismiss() {
                System.out.println("dismiss");
                timer.cancel();
            }
        });
        /*
        Listener l = new Listener() {
            @Override
            public void dismiss() {
                timer.cancel();
            }
        };
        dialog.setListener(l);
        */

        //dialog.notifyListener();
    }

    public void trigger() {
        dialog.notifyListener();
    }

    public static void main(String[] args) {
        TimerTest t = new TimerTest();
        t.test();
        t.trigger();
    }

    private Dialog dialog = new Dialog();
}

interface Listener {
    public void dismiss();
}

class Dialog {
    Listener listener;
    public void setListener(Listener l) {
        listener = l;
    }

    public void notifyListener() {
        listener.dismiss();
    }
}
