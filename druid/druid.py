#!/usr/bin/python
#
# Java and Android Analysis Framework
# Copyright (C) 2017 Jingbo Lu, Yulei Sui
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the
# Free Software Foundation, Inc., 59 Temple Place - Suite 330,
# Boston, MA 02111-1307, USA.

import os, sys, shutil
class Color:
	RESET  = '\033[0m'
	BOLD   = '\033[1m'
	GREEN  = '\033[32m'
	YELLOW = '\033[33m'
	CYAN   = '\033[36m'
	WHITE  = '\033[37m'
	RED    = '\033[91m'
	@staticmethod
	def changeToNT():
		Color.RESET  = ''
		Color.BOLD   = ''
		Color.GREEN  = ''
		Color.YELLOW = ''
		Color.CYAN   = ''
		Color.WHITE  = ''
		Color.RED    = ''
if(os.name == 'nt'):
	Color.changeToNT()
	cmdsep='&&'
else:
	cmdsep='\n'
#HOME
DRUID_HOME = os.getenv('DRUID_HOME', '.')
AAFE = os.path.join(DRUID_HOME, 'aafe')
#heapsize
XMX = '-Xmx64g'
#FLOWDROID
FLOWDROIDOPTIONS='--nostatic --aplength 1 --aliasflowins --layoutmode none --noarraysize'
sdk = os.path.join(DRUID_HOME, 'lib', 'sdk', 'platforms') #Default SDK
FLOWDROIDCLASSPATH = os.pathsep.join([
	os.path.abspath(os.path.join(AAFE, 'soot-infoflow.jar')),
	os.path.abspath(os.path.join(AAFE, 'soot-infoflow-android.jar')),
	os.path.abspath(os.path.join(DRUID_HOME, 'lib', 'soot-trunk.jar')),
	os.path.abspath(os.path.join(DRUID_HOME, 'lib', 'axml-2.0.jar')),
	'.',
	])
aafe_cmd = ( 'cd '+AAFE+cmdsep+'java -cp '
	+ FLOWDROIDCLASSPATH											# FLOWDROIDCLASSPATH
	+ ' %s'	%XMX							# heap size
	+ ' soot.jimple.infoflow.android.TestApps.Test' # main class
	+ ' %s '								# apk directory
	+ '%s '							# android platforms
	+ FLOWDROIDOPTIONS
	+ os.path.join(' > %s', 'output.txt')
	)
#RUNJA
CLASSPATH = os.pathsep.join([
	os.path.join(DRUID_HOME, 'config-files'),
	os.path.join(DRUID_HOME, 'classes'),
	os.path.join(DRUID_HOME, 'lib', 'commons-cli-1.2.jar'),
	os.path.join(DRUID_HOME, 'lib', 'commons-io-2.4.jar'),
	os.path.join(DRUID_HOME, 'lib', 'soot-trunk.jar'),
	os.path.join(DRUID_HOME, 'lib', 'string.jar'),
	os.path.join(DRUID_HOME, 'lib', 'automaton.jar'),
	os.path.join(DRUID_HOME, 'lib', 'gson-2.7.jar'),
	])
runJava_cmd = 'java -Xms1g %s -cp ' + CLASSPATH + ' driver.Main %s'
#OPTIONS
def makeup(str):
	return ' '*(30-len(str))
def bioption(opt, arg, des):
	return Color.BOLD + Color.YELLOW + opt +' '+ Color.GREEN + arg + Color.WHITE + makeup(opt + arg) + des + Color.RESET +'\n'
def option(opt, des):
	return bioption(opt, '', des)
OPTIONMESSAGE = 'The valid OPTIONs are:\n'\
	+ option('-help|-h', 'print this message.')\
	+ bioption('-apk', '<APKFILE>', 'specify the apk to be analyzed.(compulsory)')\
	+ bioption('-sdk', '<SDK_Platforms_Path>', 'used by FlowDroid. (default: android 19-25)')\
	+ bioption('-apppath', '<CLASSDIR or APPJAR>', 'specify the app to be analyzed. (default: .)')\
	+ bioption('-mainclass', '<MAINCLASS>', 'specify the mainclass. (compulsory when more than one main method exist)')\
	+ bioption('-jre', '<JREDIR>', 'specify the jre to be analyzed. (default: jre1.6 provided.)')\
	+ bioption('-libpath', '<LIBPATH>', 'specify the lib used by the app.')\
	+ bioption('-kobjsens', '<K>', 'run with K-object sensitivity.')\
	+ option('-reflection', 'enable inference reflection analysis. (now works only for k=0 & jre 1.6 or below)')\
	+ option('-lhm', 'enable lazy heap modeling. (works only when reflection is enabled)')\
	+ option('-stringanalysis', 'enable string analysis. (useful when reflection is on)')\
	+ bioption('-reflectionlog', '<LOGFILE>', 'specify the log file for resolving reflective call sites.')\
	+ option('-singleentry', 'A lightweight mode with only one main method entry. (default value: false)')\
	+ option('-originalName', 'use original Java names in jimples. (default value: false)')\
	+ option('-apicalldepth', 'Depth to traverse into API for call graph building, -1 is follow all edges (default value: -1)')\
	+ bioption('-include', '<PACKAGE>', 'include specified package excluded by default.')\
	+ bioption('-exclude', '<PACKAGE>', 'exclude all classes under the specified package.')\
	+ option('-includeall', 'include all packages excluded by default.')\
	+ option('-imprecisestrings', 'Turn off precision for all strings, FAST and IMPRECISE (default value: false)')\
	+ option('-stringconstants', 'Propagate all string constants (default value: true)')\
	+ option('-limitcontextforstrings', 'Limit heap context to 1 for Strings in PTA (default value: false)')\
	+ option('-typesforcontext', 'Use types (instead of alloc sites) for object sensitive context elements > 1 (default value: false)')\
	+ option('-noclinitcontext', 'PTA will not add special context for static inits (default value: false)')\
	+ option('-extraarraycontext', 'add more context for arrays (default value: false)')\
	+ option('-debug', 'print out all G.v() information. (default value: false)')\
	+ option('-dumpjimple', 'dump appclasses to jimple. (default value: false)')\
	+ option('-dumppts', 'dump pta to a file. (default value: false)')\
	+ option('-dumpCallGraph', 'print a CG graph. (default value: false)')\
	+ option('-dumppag', 'print a PAG graph. (default value: false)')\
	+ option('-dumphtml', 'dump a PAG graph to an html. (default value: false)')\
	+ bioption('-Xmx', '\b<MAX>', '  Specify the maximum size, in bytes, of the memory allocation pool.')\

# transform a list of apps
def transform_apps(app):
	if not os.path.exists(app):
		sys.exit('apk file ' + app + ' does not exist!')
	output_dir = os.path.splitext(app)[0]
	app_name = os.path.basename(output_dir)
	if os.path.exists(os.path.join(output_dir, 'dummyMainClass.class')):
		print ('App %s has been transformed, skip this.' % app_name)
		return
	if os.path.exists(output_dir):
		shutil.rmtree(output_dir)
	os.mkdir(output_dir)
	print ('# Transform app ', app_name)
	cmd = aafe_cmd % (app, sdk, output_dir)
#	print (cmd)
	if(os.system(cmd)!=0):
		sys.exit('aafe failed. Exit.')


	
if '-help' in sys.argv or '-h' in sys.argv:
	sys.exit(OPTIONMESSAGE)
if(len(sys.argv) < 3):
	sys.exit('Not enough options! '+OPTIONMESSAGE)
#Transform apk
if '-sdk' in sys.argv:
	sdk = sys.argv[sys.argv.index('-sdk')+1]
	sys.argv.remove('-sdk')
	sys.argv.remove(sdk)
sdk = os.path.abspath(sdk)
if '-apk' in sys.argv:
	apk=sys.argv[sys.argv.index('-apk')+1]
	sys.argv.remove('-apk')
	sys.argv.remove(apk)
	apk = os.path.abspath(apk)
	transform_apps(apk)
	sys.argv.append('-apppath')
	sys.argv.append(os.path.splitext(apk)[0])
	sys.argv.append('-libpath')
	sys.argv.append(os.path.join(AAFE, 'android-lib'))
	sys.argv.append('-mainclass')
	sys.argv.append('dummyMainClass')
#prepare javacmd args
elif not ('-jre' in sys.argv or '-libpath' in sys.argv and sys.argv[sys.argv.index('-libpath')+1].endswith('android-lib')):
	sys.argv.append('-jre')
	sys.argv.append(os.path.join(DRUID_HOME, 'lib', 'jre', 'jre1.6.0_45'))
i = 1
while ( i < len(sys.argv) ):
	if sys.argv[i].startswith('-Xmx'):
		XMX = sys.argv[i]
		sys.argv[i] = ''
	i += 1
if __name__ == "__main__":
	os.system(runJava_cmd %(XMX, ' '.join(sys.argv[1:])))
