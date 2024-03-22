#ifndef PEREZREVERSEKILLER_UTILS_H
#define PEREZREVERSEKILLER_UTILS_H
#include <stdint.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif
    intptr_t openFd(intptr_t fd, const char *path, intptr_t flag);
    char *j2c(JNIEnv *env, jstring jstr);

#ifdef __cplusplus
};
#endif

#endif //PEREZREVERSEKILLER_UTILS_H
