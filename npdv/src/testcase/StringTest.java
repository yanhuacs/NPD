package testcase;

import java.io.File;

public class StringTest {

    public static void main(String[] args) {
        FileInfo info = new FileInfo(new String(""));
        /*
        String oldPath = info.getSendFilePath();
        //int index = oldPath.lastIndexOf(".");
        String newPath = oldPath.substring(2);
        newPath.length();
        */
        String number = info.getMatchPhone("abcd");
        number.length();

        String number2 = info.getMatchPhone(null);
        number2.length();


        String number3 = info.testIsEmpty("abc");
        number3.length();

    }
}

class FileInfo {
    private String sendFilePath;

    public FileInfo() {

    }

    public FileInfo(String path) {
        sendFilePath = path;
    }

    public String getSendFilePath() {
        return sendFilePath;
    }

    public String getMatchPhone(String num) {
        if (null == num)
            return null;
        String number = stripSeparators(num);
        if (number.length() > 10) {
            number = number.substring(5);
        }
        return number;
    }

    public String stripSeparators(String number) {
        if (null == number)
            return null;
        int len = number.length();
        StringBuilder sb = new StringBuilder(len);
        return sb.toString();
    }

    public boolean isEmpty(String num) {
        if (num == null) return true;
        else return false;
    }
    public String testIsEmpty(String num) {
        //if (isEmpty(num))
        //    return null;
        if (num == null)
            return null;
        if (num.length() > 10) {
            return num;
        } else
            return "default";
    }
}
