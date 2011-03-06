#!/bin/bash -x

if [ "$(uname)" == "Darwin" ]; then
  os=mac_x86
else
  os=linux
fi
ASDK_ROOT=~/Downloads/android-sdk-${os}
MIN_ANDROID_RELEASE=4
MAX_ANDROID_RELEASE=11
JAVA_ROOT=/usr/bin #JAVA_ROOT=/usr/lib/jvm/java-6-sun/bin
RELEASE_KEYSTORE=~/android-market.keystore


# Various Android tools.
AAPT=${ASDK_ROOT}/platform-tools/aapt
ADB=${ASDK_ROOT}/platform-tools/adb
APKBUILDER=${ASDK_ROOT}/tools/apkbuilder
DX=${ASDK_ROOT}/platform-tools/dx
ZIPALIGN=${ASDK_ROOT}/tools/zipalign

# Various JDK tools.
JAVAC=${JAVA_ROOT}/javac
JARSIGNER=${JAVA_ROOT}/jarsigner

# The Android class library.
ANDROID_CLASSES_JAR=${ASDK_ROOT}/platforms/android-${MIN_ANDROID_RELEASE}/android.jar
ANDROID_RESOURCES_JAR=${ASDK_ROOT}/platforms/android-${MAX_ANDROID_RELEASE}/android.jar

# Find out what we're building from the manifest.
APP_NAME=`sed -n 's/.*name="app_name">\(.*\)<.*/\1/ p' res/values/strings.xml | sed 's/[ ]//'`
APP_PACKAGE=`sed -n 's/.*package="\([^"]*\)".*/\1/ p' AndroidManifest.xml`
APP_VERSION=`sed -n 's/.*android:versionName="\(.*\)".*/\1/ p' AndroidManifest.xml`

# javac options.
JAVAC_FLAGS="-d .generated/classes/"
JAVAC_FLAGS="${JAVAC_FLAGS} -sourcepath src/"
JAVAC_FLAGS="${JAVAC_FLAGS} -bootclasspath ${ANDROID_CLASSES_JAR}"
JAVAC_FLAGS="${JAVAC_FLAGS} -g"
# Turn on warnings.
JAVAC_FLAGS="${JAVAC_FLAGS} -deprecation"
JAVAC_FLAGS="${JAVAC_FLAGS} -Xlint:all -Xlint:-serial"
# We're only guaranteed that dex supports Java 5 .class files.
JAVAC_FLAGS="${JAVAC_FLAGS} -target 1.5"
# Ensure we give a clear error if the user attempts to use anything older than Java 5.
JAVAC_FLAGS="${JAVAC_FLAGS} -source 1.5"
# javac(1) warns if you build source containing characters unrepresentable
# in your locale. Although we all use UTF-8 locales, we can't guarantee that
# everyone else does, so let the compiler know that our source is in UTF-8.
# Android usually uses ASCII, but this is 2009.
JAVAC_FLAGS="${JAVAC_FLAGS} -encoding UTF-8"


function usage() {
    echo "usage: $0 [clean|debug|release]" > /dev/stderr
    exit 1
}

# What does the user want us to do?
if [ $# -eq "0" ]; then
    target=debug
elif [ $# -eq "1" ]; then
    target=$1
    if [[ "$target" != "clean" && "$target" != "debug" && "$target" != "release" ]]; then
        usage
    fi
else
    usage
fi

# "clean" is easy, and unrelated to "debug" and "release"...
if [ "$target" == "clean" ]; then
    echo "-- Removing generated files..."
    rm -rf .generated || exit 1
    exit 0
fi

echo "-- Generating R.java and Manifest.java from resources..."
mkdir -p gen
${AAPT} package -m -J gen -M AndroidManifest.xml -S res -I ${ANDROID_RESOURCES_JAR} || exit 1

# FIXME: add aidl support

echo "-- Compiling Java source..."
JAVA_SOURCE_FILES=`find src gen -type f -name "*.java"`
rm -rf .generated/classes && \
  mkdir -p .generated/classes && \
  ${JAVAC} ${JAVAC_FLAGS} ${JAVA_SOURCE_FILES} || exit 1

echo "-- Building classes.dex..."
dex_out=.generated/classes.dex
${DX} --dex --output=${dex_out} .generated/classes || exit 1

echo "-- Packaging resources..."
aapt_out=.generated/${APP_NAME}.ap_
assets="" # An assets directory is optional.
if [ -d assets ]; then
  assets="-A assets"
fi
${AAPT} package -f -M AndroidManifest.xml ${assets} -S res -I ${ANDROID_RESOURCES_JAR} -F ${aapt_out} || exit 1

echo "-- Creating ${target} apk..."
if [ "$target" == "release" ]; then
  # For release we need an unsigned apk so we can sign it with our release key later.
  # By default, apkbuilder will use our debug key.
  extra_apkbuilder_flags=-u
  apk_suffix=unsigned
else
  apk_suffix=debug
fi
apkbuilder_out=.generated/${APP_NAME}-${apk_suffix}.apk
${APKBUILDER} ${apkbuilder_out} ${extra_apkbuilder_flags} -v -f ${dex_out} -z ${aapt_out} || exit 1

if [ "$target" == "debug" ]; then
  # FIXME: does debug always imply install, or should they be two separate targets?
  # FIXME: you might want to choose a destination
  echo "-- Installing apk..."
  #${ADB} shell pm uninstall -k ${APP_PACKAGE} || exit 1
  ${ADB} install -r ${apkbuilder_out} || exit 1
  # That's the end of the line for "debug"...
  exit 0
fi

# Sign the apk and check the signing worked.
jarsigner_out=.generated/${APP_NAME}-signed.apk
${JARSIGNER} -verbose -keystore ${RELEASE_KEYSTORE} -signedjar ${jarsigner_out} ${apkbuilder_out} android-release-key || exit 1
${JARSIGNER} -verify ${jarsigner_out} || exit 1

# Zipalign the signed apk.
zipalign_out=.generated/${APP_NAME}-aligned.apk
${ZIPALIGN} -v 4 ${jarsigner_out} ${zipalign_out} || exit 1
${ZIPALIGN} -c -v 4 ${zipalign_out} || exit 1

# Make a safe copy. The signed, aligned apk is something you want to keep.
# FIXME: automated upload to code.google.com if applicable.
mv ${zipalign_out} ${APP_NAME}-${APP_VERSION}.apk || exit 1
