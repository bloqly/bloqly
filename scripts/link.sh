#!/usr/bin/env bash

JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-10.jdk/Contents/Home

${JAVA_HOME}/bin/jlink  --compress=2 --no-header-files --no-man-pages --output ../release/java --add-modules java.sql,jdk.charsets,java.logging,java.xml,jdk.xml.dom,jdk.net,java.naming,java.prefs,jdk.naming.rmi,jdk.zipfs,java.base,jdk.crypto.ec,jdk.management.agent,java.management,java.sql.rowset,jdk.jsobject,jdk.unsupported,jdk.scripting.nashorn,java.instrument,jdk.management,jdk.security.auth,java.scripting,jdk.dynalink,java.management.rmi,jdk.localedata