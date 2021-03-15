#!/usr/bin/env bash

if [ ! -f ${CI_GITLAB_PATH}/${CI_JOB_NAME}-passed ] ; then
    BUILD_STATUS=failed ${CI_SCRIPTS_PATH}/build-status.sh;
else
    BUILD_STATUS=passed ${CI_SCRIPTS_PATH}/build-status.sh;
fi
