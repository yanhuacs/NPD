#!/usr/bin/python
import os

benchmarks=['antlr', 'chart', 'eclipse', 'fop', 'luindex', 'lusearch', 'pmd', 'xalan']

def runNPV(bench):
    run_command='./druid-npda.py -singleentry -apppath dacapo-bench/' + bench + '.jar' \
                + ' -mainclass dacapo.%s.Main'%bench \
                + ' -libpath dacapo-bench/' + bench + '-deps.jar'
    print 'Command: ' + run_command
    print 'Now running ' + bench + ' ...'
    os.system(run_command)

if __name__ == '__main__':
    for bench in benchmarks:
        runNPV(bench)
