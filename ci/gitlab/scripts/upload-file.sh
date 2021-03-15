#!/usr/bin/env bash

# Publish a file to the Bitbucket Cloud "Download" section
# For example https://bitbucket.org/okapiframework/okapi/downloads/

if [ -z "$BITBUCKET_ACCESS_TOKEN" ]; then
   echo "ERROR: BITBUCKET_ACCESS_TOKEN is not set"
   exit 1
fi
if [ -z "$BITBUCKET_USERNAME" ]; then
    echo "ERROR: BITBUCKET_USERNAME is not set"
    exit 1
fi
if [ -z "$BITBUCKET_NAMESPACE" ]; then
    echo "Setting BITBUCKET_NAMESPACE to $CI_PROJECT_NAMESPACE"
    BITBUCKET_NAMESPACE=$CI_PROJECT_NAMESPACE
fi
if [ -z "$BITBUCKET_REPOSITORY" ]; then
    echo "Setting BITBUCKET_REPOSITORY to $CI_PROJECT_NAME"
    BITBUCKET_REPOSITORY=$CI_PROJECT_NAME
fi

if [ -z "$FILE_TO_UPLOAD" ]; then
    echo "Setting FILE_TO_UPLOAD to $1"
    FILE_TO_UPLOAD=$1
fi
if [ ! -f "$FILE_TO_UPLOAD" ]; then
    echo "Can't find upload file $FILE_TO_UPLOAD"
    exit 1
fi

BITBUCKET_API_ROOT="https://api.bitbucket.org/2.0"
BITBUCKET_DOWNLOADS_API="$BITBUCKET_API_ROOT/repositories/$BITBUCKET_NAMESPACE/$BITBUCKET_REPOSITORY/downloads"

# We would expect HTTP status 201, meaning "Created success"
echo "Uploading $FILE_TO_UPLOAD to $BITBUCKET_DOWNLOADS_API..."
curl --request POST $BITBUCKET_DOWNLOADS_API \
  --user $BITBUCKET_USERNAME:$BITBUCKET_ACCESS_TOKEN \
  --write-out "[ Code:%{http_code}  Time:%{time_total} sec ]" \
  --silent \
  --show-error \
  --output /dev/null \
  -F files=@$FILE_TO_UPLOAD
