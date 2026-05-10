#include <cerrno>

#include "jni.h"
#include <cstdint>
#include <cstdio>
#include <cstring>

#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include "revpi/piControl.h"
#include "generated/org_stm_pegelhub_connector_ma_jni_RevPiReaderImpl.h"

namespace {
    int handle = -1;

    int ensureOpen() {
        if (handle >= 0) return handle;
        const int h = open(PICONTROL_DEVICE, O_RDONLY);
        if (h < 0) return -1;
        handle = h;
        return handle;
    }


    void throwIo(JNIEnv* env, const char* msg) {
        jclass ex = env->FindClass("java/io/IOException");
        if (!ex) return;

        char buf[256];
        int e = errno;
        if (e != 0) {
            std::snprintf(buf, sizeof(buf), "%s (errno=%d: %s)", msg, e, std::strerror(e));
        } else {
            std::snprintf(buf, sizeof(buf), "%s", msg);
        }
        env->ThrowNew(ex, buf);
    }
} // helpers

JNIEXPORT jint JNICALL Java_org_stm_pegelhub_connector_ma_jni_RevPiReaderImpl_resolveOffsetByName
(JNIEnv *env, jobject /*obj*/, jstring jname) {
    if (!jname) {
        errno = 0;
        throwIo(env, "Variable name is null");
        return 0;
    }

    const char *inputName = env->GetStringUTFChars(jname, nullptr);
    if (!inputName) {
        errno = 0;
        throwIo(env, "Failed to get UTF chars");
        return 0;
    }

    SPIVariable inputStruct{};
    std::strncpy(inputStruct.strVarName, inputName, sizeof(inputStruct.strVarName) - 1);
    inputStruct.strVarName[sizeof(inputStruct.strVarName) - 1] = '\0';
    env->ReleaseStringUTFChars(jname, inputName);

    if (ensureOpen() < 0) {
        throwIo(env, "open(/dev/piControl0) failed");
        return 0;
    }

    if (ioctl(handle, KB_FIND_VARIABLE, &inputStruct) < 0) {
        throwIo(env, "KB_FIND_VARIABLE ioctl failed");
        return 0;
    }

    return inputStruct.i16uAddress;
}

JNIEXPORT jint JNICALL Java_org_stm_pegelhub_connector_ma_jni_RevPiReaderImpl_readFromOffset
(JNIEnv *env, jobject /*obj*/, jint offset) {
    if (offset < 0) {
        errno = 0;
        throwIo(env, "Negative offset");
        return 0;
    }

    if (ensureOpen() < 0) {
        throwIo(env, "open(/dev/piControl0) failed");
        return 0;
    }

    uint8_t buf[2] = {0, 0};
    const ssize_t n = pread(handle, buf, 2, offset);

    if (n < 0) {
        throwIo(env, "pread failed");
        return 0;
    }
    if (n != 2) {
        errno = 0;
        throwIo(env, "Short read: expected 2 bytes");
        return 0;
    }

    const uint16_t value = static_cast<uint16_t>(buf[0]) | static_cast<uint16_t>(buf[1]) << 8;
    return value;
}

JNIEXPORT void JNICALL Java_org_stm_pegelhub_connector_ma_jni_RevPiReaderImpl_close
(JNIEnv *env, jobject /*obj*/) {
    if (handle >= 0) {
        const int fd = handle;
        handle = -1;
        close(fd);
    }
}
