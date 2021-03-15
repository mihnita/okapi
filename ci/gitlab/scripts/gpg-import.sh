#!/usr/bin/env bash

OPENSSL_CLI_OPTS="enc -aes-256-cbc -K ${OPENSSL_ENC_KEY} -iv ${OPENSSL_ENC_IV}"

openssl ${OPENSSL_CLI_OPTS} -d -in ${CI_GITLAB_PATH}/code-signing-key.asc.enc -out ${CI_GITLAB_PATH}/code-signing-key.asc

