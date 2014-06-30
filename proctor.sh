#!/bin/bash
# Simple shell script to run proctor via maven from the command line, within the build environment.
# moxious
MAVEN=mvn
GOAL=exec:java
MAINCLASS=-DmainClass=org.mitre.provenance.capture.linux.PROCtor
ARGS="-DcommandlineArgs=\"$@\""

echo $MAVEN $GOAL $MAINCLASS $ARGS
$MAVEN $GOAL $MAINCLASS $ARGS

