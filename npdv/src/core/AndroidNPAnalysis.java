package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

import static soot.SootClass.SIGNATURES;

public class AndroidNPAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(AndroidNPAnalysis.class);

    public static void main(final String[] args) {
        if (args.length < 2)
            return;
        // args: anroid_app.apk anroid-platform -apppath android_app -libpath libpath -mainclass dummyMainClass
        logger.info("Start processing android apk file...");

        String androidFileLoc = args[0];
        String androidJarFile = args[1];
        //System.setProperty("user.dir", "aafe");
        //String curDir = new File("").getAbsoluteFile().getAbsolutePath();
        //curDir = curDir + "/druid/aafe";
        //System.setProperty("user.dir", curDir);
        //logger.info("Current working dir: " + curDir);

        SetupApplication preAnalysis = new SetupApplication(androidJarFile, androidFileLoc);
        preAnalysis.constructCallgraph();

        logger.info("Finish processing android apk file...");
        String[] restArgs = Arrays.copyOfRange(args, 2, args.length);
        logger.info("Start analyzing android apk's NPE...");
        Scene.v().addBasicClass("java.security.PrivilegedActionException", SIGNATURES);
        core.NPVDriver.main(restArgs);
    }
}
