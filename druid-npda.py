#!/usr/bin/python
#
# Java and Android Analysis Framework
# Copyright (C) 2017 Xinwei Xie, Jingbo Lu and Yulei Sui
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
import argparse
sys.path.insert(0, 'druid')

PROJECT_HOME = os.getcwd()
DRUID_HOME = os.path.join(PROJECT_HOME, 'druid')
CLASSPATH = os.path.join(PROJECT_HOME, 'npda.jar')
AAFE_PATH = os.path.join(DRUID_HOME, 'aafe')
JRELIB = os.path.join(DRUID_HOME, 'lib', 'jre', 'jre1.6.0_45')
FLOWDROID_OPTIONS = ' --nostatic --aplength 1 --aliasflowins --layoutmode none --noarraysize '

#loglevel='DEBUG'
#loglevel='INFO'
#loglevel='WARN'
#loglevel='ERROR'

log = '-Dorg.slf4j.simpleLogger.defaultLogLevel=%s'
android_sdk = os.path.join(DRUID_HOME, 'lib', 'sdk', 'platforms')
singlethread_class = 'core.NPVDriver'
multithread_class = 'thread.NPExecutor'
run_android = 'java -Xms1g -Xmx256g -XX:+UseConcMarkSweepGC ' + log + ' -cp ' + CLASSPATH + ' %s -apppath %s -libpath %s -mainclass dummyMainClass'
run_java = 'java -Xms1g ' + log + ' -cp ' + CLASSPATH + ' %s -apppath %s -mainclass %s -jre ' + JRELIB
aafe_cmd = 'java -Xms1g -cp ' + CLASSPATH + ' soot.jimple.infoflow.android.TestApps.Test %s %s ' + FLOWDROID_OPTIONS + os.path.join(' > %s', 'output.txt')

def transform_apk(apk, sdk, aafe_path, project_home):
	if not os.path.exists(apk):
		sys.exit('apk file ' + apk + ' does not exist!')

	output_dir = os.path.splitext(apk)[0]
	apk_name = os.path.basename(output_dir)
	if os.path.exists(os.path.join(output_dir, 'dummyMainClass.class')) or os.path.exists(os.path.join(output_dir, 'dummyMainClass.jimple')):
		print('Apk % s has been transformed, skip it.' %apk_name)
		return
	if os.path.exists(output_dir):
		shutil.rmtree(output_dir)
	os.mkdir(output_dir)
	print ('Transforming apk ' + apk_name + ' ...')
	#print ('Apk ' + apk + ', sdk ' + sdk + ', outputdir ' + output_dir)
	os.chdir(aafe_path)
	cmd = aafe_cmd % (apk, sdk, output_dir)
	print ('Transforming cmd ' + cmd)
	if (os.system(cmd) != 0):
		sys.exit('aafe transforming failed, exit.')
	os.chdir(project_home)

parser = argparse.ArgumentParser(description="Null Pointer Detector for Android and Java")
parser.add_argument('-apk', metavar='APKFILE', help='specify apk file')
parser.add_argument('-thread', metavar='NUMTHREADS', help='specify number of threads')
parser.add_argument('-apppath', help='specify apppath')
parser.add_argument('-mainclass', help='specify main class')
parser.add_argument('-jre', metavar='JREPATH', help='specify jre path')
parser.add_argument('-kobjsens', metavar='K', help='run with K-object sensitivity, default value: 0')
parser.add_argument('-sdk', metavar='SDKPATH', help='specify android platform sdk.')
parser.add_argument('-libpath', metavar='LIBPATH', help='specify lib path used by the app.')
parser.add_argument('-reflectionlog', metavar='LOGFILE', help='specify the log file for resolving reflective call sites.')
parser.add_argument('-include', metavar='PACKAGE', help='include specified packages excluded by default.')
parser.add_argument('-exclude', metavar='PACKAGE', help='exclude all classes of specified packages.')
parser.add_argument('-log', metavar='LOGLEVEL', help='log verbose level, includes: error, warn, info, debug.')
#parser.add_argument('-Xmx', help='')

parser.add_argument('-reflection', help='enable inference reflection analysis', action='store_true')
parser.add_argument('-lhm', help='enable lazy heap modeling. (works only when reflection is enabled)', action='store_true')
parser.add_argument('-stringanalysis', help='enable string analysis. (useful when reflection is on)', action='store_true')
#parser.add_argument('-singleentry', help='a lightweight mode with only one main entry.', action='store_true')
parser.add_argument('-originalName', help='use original Java names in jimples.', action='store_true')
parser.add_argument('-includeall', help='include all packages excluded by default.', action='store_true')
parser.add_argument('-imprecisestrings', help='turn off precision for all strings, FAST and IMPRECISE.', action='store_true')
parser.add_argument('-stringconstants', help='propagate all string constants.', action='store_true')
parser.add_argument('-limitcontextforstrings', help='propagate all string constants.', action='store_true')
parser.add_argument('-typesforcontext', help='use types (instead of alloc sites) for object sensitive context elements > 1.', action='store_true')
parser.add_argument('-noclinitcontext', help='PTA will not add special context for static inits.', action='store_true')
parser.add_argument('-extraarraycontext', help='add more contexts for arrays.', action='store_true')
parser.add_argument('-debug', help='print all G.v() information.', action='store_true')
parser.add_argument('-dumpjimple', help='dump appclasses to jimple.', action='store_true')
parser.add_argument('-dumppts', help='dump PTA to file.', action='store_true')
parser.add_argument('-dumpCallGraph', help='dump callgraph to file', action='store_true')
parser.add_argument('-dumppag', help='dump pag graph to file', action='store_true')
parser.add_argument('-dumphtml', help='dump pag graph to html', action='store_true')

#args, unknown = parser.parse_known_args()
args = parser.parse_args()

cmd = ''
analyze_apk = False
num_threads = 1
if args.thread:
	num_threads = args.thread
	tool_mainclass = '-Dthread.NPExecutor.nthreads=' + num_threads + ' ' + multithread_class
	del args.thread
else:
	tool_mainclass = singlethread_class

if args.apk:
	apk = args.apk
	if not apk.endswith(".apk"):
		parser.error('apk file: ' + apk + ' does not end with .apk!')
	apk = os.path.abspath(apk)
	if not os.path.exists(apk):
		parser.error('apk file: ' + apk + ' does not exist!')

	output_dir = os.path.splitext(apk)[0]
	apppath = output_dir
	libpath = os.path.join(AAFE_PATH, 'android-lib')
	analyze_apk = True
	del args.apk
	if args.log:
		loglevel=args.log.upper()
		del args.log
	else:
		loglevel='INFO'
	sdk = ''
	if args.sdk:
		sdk = arg.sdk
	else:
		#sdk = ps.path.join(DRUID_HOME, 'lib', 'sdk', 'platforms')
		sdk = android_sdk
	transform_apk(apk, sdk, AAFE_PATH, PROJECT_HOME)
	apppath = os.path.splitext(apk)[0]
	cmd = run_android % (loglevel, tool_mainclass, apppath, libpath)
elif args.apppath:
	if not args.mainclass:
		parser.error('the -apppath argument requires -mainclass to specify main class!')

	apppath = args.apppath
	if not os.path.exists(apppath):
		parser.error('apppath: ' + apppath + ' does not exist!')
	mainclass = args.mainclass
	del args.apppath
	del args.mainclass
	if args.log:
		loglevel=args.log.upper()
		del args.log
	else:
		loglevel='INFO'
	cmd = run_java % (loglevel, tool_mainclass, apppath, mainclass)
else:
	parser.print_help()

args_map = vars(args)
append_cmd = ''
for key,value in args_map.items():
	if value is None:
		continue
	else:
		if value is True:
			append_cmd += '-' + key + ' '
			#append_cmd += ' '
		elif value is not False:
			append_cmd += '-' + key + ' ' + str(value) + ' '
			#append_cmd += ' '

if cmd and cmd.strip():
	cmd += ' ' + append_cmd + ' -singleentry'
	print 'running following cmd:\n' + cmd
	if analyze_apk is True:
		#os.chdir(AAFE_PATH)
		os.system(cmd)
		#os.chdir(PROJECT_HOME)
	else:
		#oschdir('.')
		os.system(cmd)
