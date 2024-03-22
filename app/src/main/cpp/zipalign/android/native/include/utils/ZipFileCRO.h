#ifndef __LIBS_ZIPFILECRO_H
#define __LIBS_ZIPFILECRO_H

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <utils/Compat.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Trivial typedef to ensure that ZipFileCRO is not treated as a simple integer.
 */
typedef void *ZipFileCRO;

/*
 * Trivial typedef to ensure that ZipEntryCRO is not treated as a simple
 * integer.  We use NULL to indicate an invalid value.
 */
typedef void *ZipEntryCRO;

extern ZipFileCRO ZipFileXRO_open(const char *path);

extern void ZipFileCRO_destroy(ZipFileCRO zip);

extern ZipEntryCRO ZipFileCRO_findEntryByName(ZipFileCRO zip,
        const char *fileName);

extern bool ZipFileCRO_getEntryInfo(ZipFileCRO zip, ZipEntryCRO entry,
                                    int *pMethod, size_t *pUncompLen,
                                    size_t *pCompLen, off64_t *pOffset, long *pModWhen, long *pCrc32);

extern bool ZipFileCRO_uncompressEntry(ZipFileCRO zip, ZipEntryCRO entry, int fd);

#ifdef __cplusplus
}
#endif

#endif /*__LIBS_ZIPFILECRO_H*/
