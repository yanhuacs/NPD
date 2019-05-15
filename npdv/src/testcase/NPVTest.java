package testcase;


import driver.SootUtils;
import core.NPVerifier;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class NPVTest {
    static final String srcPath = "src/npv/testcase/tests";
    static final List<Object[]> mainList=new ArrayList<Object[]>();
    static final Map<String, Integer> testLibrary = new HashMap<String, Integer>();

    static {
        testLibrary.put("npv.testcase.tests.Intra", 1);

        testLibrary.put("npv.testcase.tests.If1", 1);
        testLibrary.put("npv.testcase.tests.If2", 2);
        testLibrary.put("npv.testcase.tests.If3", 1);

        testLibrary.put("npv.testcase.tests.Switch1", 1);
        testLibrary.put("npv.testcase.tests.Switch2", 1);

        testLibrary.put("npv.testcase.tests.Ptr1", 1);
        testLibrary.put("npv.testcase.tests.Ptr2", 1);
        testLibrary.put("npv.testcase.tests.Ptr3", 2);
        testLibrary.put("npv.testcase.tests.Ptr4", 1);
        testLibrary.put("npv.testcase.tests.Ptr5", 1);
        testLibrary.put("npv.testcase.tests.Ptr6", 3);
        testLibrary.put("npv.testcase.tests.Ptr7", 1);

        testLibrary.put("npv.testcase.tests.IrrelevantConstraint1", 1);
        testLibrary.put("npv.testcase.tests.IrrelevantConstraint2", 1);

        testLibrary.put("npv.testcase.tests.Loop1", 0);
        testLibrary.put("npv.testcase.tests.Loop2", 0);
        testLibrary.put("npv.testcase.tests.Loop3", 1);
        testLibrary.put("npv.testcase.tests.Loop4", 0);
        testLibrary.put("npv.testcase.tests.Loop5", 1);
        testLibrary.put("npv.testcase.tests.Loop6", 0);

        testLibrary.put("npv.testcase.tests.Array1", 2);
        testLibrary.put("npv.testcase.tests.Array2", 2);
        testLibrary.put("npv.testcase.tests.Array3", 0);
        testLibrary.put("npv.testcase.tests.Array4", 2);

        testLibrary.put("npv.testcase.tests.Inter1", 0);
        testLibrary.put("npv.testcase.tests.Inter2", 2);
        testLibrary.put("npv.testcase.tests.Inter3", 0);
        testLibrary.put("npv.testcase.tests.Inter4", 1);
        testLibrary.put("npv.testcase.tests.Inter5", 1);
        testLibrary.put("npv.testcase.tests.Inter6", 1);
        testLibrary.put("npv.testcase.tests.Inter7", 2);
        testLibrary.put("npv.testcase.tests.Inter8", 1);
        testLibrary.put("npv.testcase.tests.Inter9", 0);
        testLibrary.put("npv.testcase.tests.Inter10", 0);
        testLibrary.put("npv.testcase.tests.Inter11", 0);

        testLibrary.put("npv.testcase.tests.Recur1", 0);
        testLibrary.put("npv.testcase.tests.Recur2", 0);
        testLibrary.put("npv.testcase.tests.Recur3", 3);
        testLibrary.put("npv.testcase.tests.Recur4", 1);

        testLibrary.put("npv.testcase.tests.Static1", 0);
        testLibrary.put("npv.testcase.tests.Static2", 1);
        testLibrary.put("npv.testcase.tests.Static3", 1);

        testLibrary.put("npv.testcase.tests.LinkedList1", 2);
        testLibrary.put("npv.testcase.tests.LinkedList2", 0);
        testLibrary.put("npv.testcase.tests.LinkedList3", 1);

        testLibrary.put("npv.testcase.tests.RetNull1", 1);
        testLibrary.put("npv.testcase.tests.RetNull2", 1);
        testLibrary.put("npv.testcase.tests.RetNull3", 1);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> prepareData() {
        if(mainList.isEmpty()){
            File filePath = new File(srcPath);
            for (File clazz : FileUtils.listFiles(filePath, new String[] { "java" }, true)) {
                String mainclass = SootUtils.fromFileToClass(clazz.toString().substring("src".length() + 1));
                /*
                if(!mainclass.startsWith("pta.utils")){
                    mainList.add(mainclass);
                }
                */
                if (testLibrary.containsKey(mainclass)) {
                    mainList.add(new Object[] {mainclass, testLibrary.get(mainclass)});
                }
            }
        }
        for(Object[] mainclass : mainList)
            System.out.println(mainclass[0]);
        System.out.println(mainList.size() + " testcases founded. Now start testing...");
        return mainList;
    }

    private String mainClassName;
    private int result;

    public NPVTest(String name, int num) {
        mainClassName = name;
        result = num;
    }

    @Test
    public void test() {
        NPVerifier.reset();
        System.out.println("Testing " + mainClassName + "...");
        invokePTA(mainClassName, new String[]{});
        assertEquals(result, NPVerifier.numOfWrong);
    }

    private static void invokePTA(String mainClass, String[] additionalArgs) {
        String[] mainArgs = new String[] { "-mainclass", // specify main class
                mainClass, // main class
        };
        additionalArgs = concat(mainArgs, additionalArgs);
        invokePTA(additionalArgs);
    }

    public static String[] concat(String[] a, String[] b) {
        String[] c = new String[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private static void invokePTA(String[] additionalArgs) {
        String[] ptaArgs = new String[] {
                "-jre",
                "jre/jre1.6.0_45",
//				"-libpath",
//				"aafe/android-lib",
                "-apppath",
                "bin/production/druid/npv/testcase",
                "-singleentry",
                "-bunch"
        };
        ptaArgs = concat(ptaArgs, additionalArgs);
        //driver.Main.main(ptaArgs);
        core.NPVDriver.main(ptaArgs);
    }
}
