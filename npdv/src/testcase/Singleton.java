package testcase;

public class Singleton {
    private boolean isNeed() {
        Config.getInstance().queryXml();
        String xml = Config.getInstance().getString();
        return xml.isEmpty();
    }

    public void checkNull() {
        if (Config.getInstance().getString() != null) {
            Config.getInstance().getString().length();
        }
    }

    public void checkNull2() {
        if (Config.getInstance() == null) {
            Config.getInstance().getString();
        }
    }

    public static void main(String[] args) {
        StaticClass.init();
        Singleton s = new Singleton();
        s.checkNull();
        s.checkNull2();
        //s.isNeed();
    }
}
class StaticClass {
    private static StaticClass inst = null;
    public static void init() {

    }

}

class Config {
    private String str;
    private static Config instance = null;
    private Config() {
        str = null;
        //queryXml();
    }

    public synchronized static Config getInstance() {
        //if (instance == null)
        //    instance = new Config();
        return instance;
    }

    public void queryXml() {
        str = new String("ttt");
    }

    public String getString() {
        return str;
    }
}
