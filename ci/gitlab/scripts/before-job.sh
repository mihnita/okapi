#!/usr/bin/env bash

rm -f ${CI_GITLAB_PATH}/${CI_JOB_NAME}-passed
BUILD_STATUS=running ${CI_SCRIPTS_PATH}/build-status.sh
