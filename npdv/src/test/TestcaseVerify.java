package test;

import core.NPVDriver;
import core.NPVerifier;
import javafx.scene.Scene;
import org.junit.*;
import soot.G;
import soot.SootResolver;
import sun.reflect.annotation.TypeAnnotation;
import testcase.*;
import thread.NPVerifyTask;

import java.util.ArrayList;
import java.util.Timer;

public class TestcaseVerify {

    public void initNPVDriver(String className) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("-mainclass"); list.add(className);
        list.add("-jre"); list.add("druid/lib/jre/jre1.6.0_45");
        list.add("-apppath"); list.add("out/production/huawei");

        String[] args = (String[]) list.toArray(new String[list.size()]);
        driver.Main.main(args);
    }


    @Before
    public void initNPVerifier() {
        NPVerifier.reset();
        G.reset();
        NPVerifier.analyzeList.clear();
        NPVerifier.wrongMap.clear();
    }

    @After
    public void cleanNPVerifier() {

    }

    //@Test
    public void testSSA() {
        initNPVDriver(SSA.class.getName());
        NPVerifier.analyzeList.add(SSA.class.getName());
        NPVDriver.run();
        Assert.assertEquals(2, NPVerifier.numOfWrong);
    }

    // further investigate
    //@Test
    public void testSingleton() {
        initNPVDriver(Singleton.class.getName());
        NPVerifier.analyzeList.add(Singleton.class.getName());
        NPVerifier.analyzeList.add("testcase.Config");
        NPVerifier.analyzeList.add("testcase.StaticClass");
        NPVDriver.run();
        Assert.assertEquals(2, NPVerifier.numOfWrong);
    }

    //@Test
    public void testInstanceOf() {
        initNPVDriver(InstanceOfTest.class.getName());
        NPVerifier.analyzeList.add("testcase.Base");
        NPVerifier.analyzeList.add("testcase.TypeA");
        NPVerifier.analyzeList.add("testcase.TypeB");
        NPVerifier.analyzeList.add("testcase.InstanceOf");
        NPVDriver.run();
        Assert.assertEquals(1, NPVerifier.numOfWrong);
    }

    @Test
    public void testString() {
        initNPVDriver(StringTest.class.getName());
        NPVerifier.analyzeList.add(StringTest.class.getName());
        NPVerifier.analyzeList.add("testcase.FileInfo");
        NPVDriver.run();
        Assert.assertEquals(0, NPVerifier.numOfWrong);
    }

    //@Test
    public void testClinit() {
        initNPVDriver(ClinitTest.class.getName());
        NPVerifier.analyzeList.add(ClinitTest.class.getName());
        NPVerifier.analyzeList.add("testcase.StaticInst");
        NPVDriver.run();
        Assert.assertEquals(0, NPVerifier.numOfWrong);
    }

    //@Test
    public void testListener() {
        initNPVDriver(TimerTest.class.getName());
        NPVerifier.analyzeList.add("testcase.TimerTest");
        NPVerifier.analyzeList.add("testcase.TimerTest$1");
        NPVerifier.analyzeList.add("testcase.TimerTest$2");
        NPVerifier.analyzeList.add("testcase.Dialog");
        NPVerifier.analyzeList.add("testcase.Listener");
        NPVDriver.run();
        Assert.assertEquals(0, NPVerifier.numOfWrong);
    }

    //@Test
    public void testSSA2() {
        String className = SSA2.class.getName();
        initNPVDriver(className);
        NPVerifier.analyzeList.add(className);
        NPVerifier.analyzeList.add("testcase.AAA");
        NPVerifier.analyzeList.add("testcase.B");
        NPVDriver.run();
        Assert.assertEquals(2, NPVerifier.numOfWrong);
    }

    //further investigate
    //@Test
    public void testLinkedList() {
        String className = LinkedList1.class.getName();
        initNPVDriver(className);
        NPVerifier.analyzeList.add(className);
        NPVerifier.analyzeList.add("testcase.Node_1");
        NPVDriver.run();
        Assert.assertEquals(2, NPVerifier.numOfWrong);
    }

    //@Test
    public void testVcTest() {
        String className = VcTest.class.getName();
        initNPVDriver(className);
        NPVerifier.analyzeList.add(className);
        NPVerifier.analyzeList.add("testcase.CheckerAssign");
        NPVerifier.analyzeList.add("testcase.ErrorReporter");
        NPVerifier.analyzeList.add("testcase.AssignExpr");
        NPVerifier.analyzeList.add("testcase.AST");
        NPVerifier.analyzeList.add("testcase.Type");
        NPVDriver.run();
        Assert.assertEquals(1, NPVerifier.numOfWrong);
    }

    // further investigate
    //@Test
    public void testRecur() {
        initNPVDriver(Recur3.class.getName());
        NPVerifier.analyzeList.add(Recur3.class.getName());
        NPVerifier.analyzeList.add("testcase.Recur3_A");
        NPVerifier.analyzeList.add("testcase.Recur3_B");
        NPVDriver.run();
        Assert.assertEquals(3, NPVerifier.numOfWrong);
    }

    //@Test
    public void testPtr() {
        String className = Ptr7.class.getName();
        initNPVDriver(className);
        NPVerifier.analyzeList.add(className);
        NPVerifier.analyzeList.add("testcase.A_Ptr7");
        NPVerifier.analyzeList.add("testcase.B_Ptr7");
        NPVDriver.run();
        Assert.assertEquals(1, NPVerifier.numOfWrong);
    }

}
