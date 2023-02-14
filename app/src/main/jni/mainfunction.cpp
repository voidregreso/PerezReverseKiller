#include <sys/syscall.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstdint>
#include "com_perez_revkiller_Features.h"
#include <android/log.h>

intptr_t openFd(intptr_t fd, const char *path, intptr_t flag) {
    return (intptr_t) syscall(__NR_openat, fd, path, flag);
}

JNIEXPORT jint JNICALL Java_com_perez_revkiller_Features_openFd
        (JNIEnv *env, jclass cls, jstring path) {
    const char* p = env->GetStringUTFChars(path, 0);
    intptr_t fd = openFd(AT_FDCWD, p, O_RDONLY);
    __android_log_print(ANDROID_LOG_INFO, "openFd", "path=%s, fd=%p", p, fd);
    return fd;
}