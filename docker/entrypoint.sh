#!/bin/sh
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

## env | sort
exec "$JAVA_HOME/bin/java" $JAVA_OPTIONS -jar "${RUN_DIR}/${LOCK_UNLOCK_JAR}" "$@"