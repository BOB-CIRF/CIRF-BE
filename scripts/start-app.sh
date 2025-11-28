#!/bin/bash

cd /home/ec2-user/cirf-be
sudo fuser -k -n tcp 8080 || true
nohup java -jar project.jar > ./output.log 2>&1 &