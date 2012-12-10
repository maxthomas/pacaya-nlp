alias runhist='history | grep run_experiments.py'

ROOT_DIR=`pwd`
#ROOT_DIR=`dirname $0`
#export CLASSPATH=$ROOT_DIR/classes:$ROOT_DIR/lib/*:/Library/gurobi301/mac64/lib/gurobi.jar:/home/hltcoe/mgormley/working/tagging/bin/gurobi301/linux64/lib/gurobi.jar
export CLASSPATH=$ROOT_DIR/classes:/home/hltcoe/mgormley/working/tagging/bin/gurobi301/linux64/lib/gurobi.jar:/home/hltcoe/mgormley/installed/ILOG/CPLEX_Studio_AcademicResearch122/cplex/lib/cplex.jar:$ROOT_DIR/lib/*
export PYTHONPATH=$ROOT_DIR/scripts:$PYTHONPATH:/Library/Python/2.6/site-packages/
export PATH=$ROOT_DIR/bin:$ROOT_DIR/dip_parse:$PATH
export PATH=$ROOT_DIR/scripts/experiments:$ROOT_DIR/scripts/experiments/core:$PATH