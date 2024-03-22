#include "com_perez_revkiller_Features.h"
#include "utils.h"
#include <cstdlib>
#include <cstdio>
#include <cstdint>
#include <cstring>
#include <fcntl.h>
#include <android/log.h>
#include <elf.h>
#include <sys/stat.h>
#include <unistd.h>


int elf_identification(int fd) {
#if defined(__x86_64__)
    Elf64_Ehdr header;
#else
    Elf32_Ehdr header;
#endif
    if(read(fd, &header, sizeof(header)) == -1)
        return 0;
    return memcmp(&header.e_ident[EI_MAG0], ELFMAG, SELFMAG) == 0;
}

bool iself(const char *f) {
    int elffd;
    if((elffd = open(f, O_RDONLY)) == -1)
        return false;
    if(!elf_identification(elffd))
        return false;
    return true;
}

JNIEXPORT jboolean JNICALL Java_com_perez_revkiller_Features_isValidElf
(JNIEnv *env, jclass cls, jstring js) {
    const char *st = env->GetStringUTFChars(js, NULL);
    if(!iself(st)) return JNI_FALSE;
    else return JNI_TRUE;
}
