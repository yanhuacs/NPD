package test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import soot.G;
import testcase.Singleton;
import thread.NPExecutor;
import thread.NPVerifyTask;

import java.util.ArrayList;

public class TestcaseVerifyMT {

    public void initNPExecutor(String className) {
        ArrayList<String> list = new ArrayList<String>();
        list.add("-mainclass"); list.add(className);
        list.add("-jre"); list.add("druid/lib/jre/jre1.6.0_45");
        list.add("-apppath"); list.add("out/production/huawei");

        String[] args = (String[]) list.toArray(new String[list.size()]);
        driver.Main.main(args);
    }

    @Before
    public void initNPVerifyTask() {
        NPVerifyTask.reset();
        G.reset();
    }

    @After
    public void cleanNPVerfiyTask() {

    }

    //@Test
    public void testClinit() {
        initNPExecutor("testcase.ClinitTest");
        NPVerifyTask.analyzeList.add("testcase.ClinitTest");
        NPVerifyTask.analyzeList.add("testcase.StaticInst");
        NPExecutor.init();
        NPExecutor.run();
    }

    //@Test
    public void testSingleton() {
        initNPExecutor(Singleton.class.getName());
        NPVerifyTask.analyzeList.add("testcase.Singleton");
        NPVerifyTask.analyzeList.add("testcase.Config");
        NPVerifyTask.analyzeList.add("testcase.StaticClass");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(2, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testInstanceOf() {
        initNPExecutor("testcase.InstanceOfTest");
        NPVerifyTask.analyzeList.add("testcase.InstanceOfTest");
        NPVerifyTask.analyzeList.add("testcase.InstanceOf");
        NPVerifyTask.analyzeList.add("testcase.Base");
        NPVerifyTask.analyzeList.add("testcase.TypeA");
        NPVerifyTask.analyzeList.add("testcase.TypeB");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(1, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testSSA() {
        initNPExecutor("testcase.SSA");
        NPVerifyTask.analyzeList.add("testcase.SSA");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(2, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testSSA2() {
        initNPExecutor("testcase.SSA2");
        NPVerifyTask.analyzeList.add("testcase.SSA2");
        NPVerifyTask.analyzeList.add("testcase.AAA");
        NPVerifyTask.analyzeList.add("testcase.B");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(2, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testString() {
        initNPExecutor("testcase.StringTest");
        NPVerifyTask.analyzeList.add("testcase.StringTest");
        NPVerifyTask.analyzeList.add("testcase.FileInfo");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(1, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testVcTest() {
        initNPExecutor("testcase.VcTest");
        NPVerifyTask.analyzeList.add("testcase.VcTest");
        NPVerifyTask.analyzeList.add("testcase.CheckerAssign");
        NPVerifyTask.analyzeList.add("testcase.ErrorReporter");
        NPVerifyTask.analyzeList.add("testcase.AssignExpr");
        NPVerifyTask.analyzeList.add("testcase.AST");
        NPVerifyTask.analyzeList.add("testcase.Type");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(1, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testPtr() {
        initNPExecutor("testcase.Ptr7");
        NPVerifyTask.analyzeList.add("testcase.Ptr7");
        NPVerifyTask.analyzeList.add("testcase.A_Ptr7");
        NPVerifyTask.analyzeList.add("testcase.B_Ptr7");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(1, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testRecur() {
        initNPExecutor("testcase.Recur3");
        NPVerifyTask.analyzeList.add("testcase.Recur3");
        NPVerifyTask.analyzeList.add("testcase.Recur3_A");
        NPVerifyTask.analyzeList.add("testcase.Recur3_B");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(1, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testLinkedList() {
        initNPExecutor("testcase.LinkedList1");
        NPVerifyTask.analyzeList.add("testcase.LinkedList1");
        NPVerifyTask.analyzeList.add("testcase.Node_1");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(0, NPVerifyTask.numOfWrong.get());
    }

    //@Test
    public void testRetNull() {
        initNPExecutor("testcase.RetNull1");
        NPVerifyTask.analyzeList.add("testcase.RetNull1");
        NPVerifyTask.analyzeList.add("testcase.RetNull1_A");
        NPVerifyTask.analyzeList.add("testcase.RetNull1_B");
        NPExecutor.init();
        NPExecutor.run();
        Assert.assertEquals(1, NPVerifyTask.numOfWrong.get());
    }

    @Test
    public void testTimer() {
        initNPExecutor("testcase.TimerTest");
        NPVerifyTask.analyzeList.add("testcase.TimerTest");
        NPVerifyTask.analyzeList.add("testcase.Dialog");
        NPVerifyTask.analyzeList.add("testcase.Listener");
        NPExecutor.init();
        NPExecutor.run();
    }
}
