#!/usr/bin/python
import os, sys
os.environ[DRUID_HOME] = os.getenv('DRUID_HOME', 'druid')
import druid
CLASSPATH = os.pathsep.join([
	os.path.join('lib', 'druid.jar'),
	])
runJava_cmd = 'java -Xms1g %s -cp ' + druid.CLASSPATH + ' driver.Main %s'
if __name__ == "__main__":
	#druid.preprare()
	os.system(runJava_cmd %(druid.XMX, ' '.join(sys.argv[1:])))