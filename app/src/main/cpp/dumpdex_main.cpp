#include <jni.h>
#include "utils.h"
#include "dumpdex/dump_dex.h"

extern "C"
JNIEXPORT jint JNICALL Java_com_perez_revkiller_Features_dumpDex(JNIEnv *env, jclass cls, jint apilevel, jstring entryclz) {
    return dump_dex(env, apilevel, j2c(env, entryclz));
}
