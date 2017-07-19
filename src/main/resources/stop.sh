#!/usr/bin/env bash
echo killing `cat $TARGET_PID`
kill `cat $TARGET_PID`
rm $TARGET_PID
