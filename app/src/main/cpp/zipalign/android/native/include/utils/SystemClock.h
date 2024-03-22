#ifndef ANDROID_UTILS_SYSTEMCLOCK_H
#define ANDROID_UTILS_SYSTEMCLOCK_H

#include <stdint.h>
#include <sys/types.h>

namespace android {

    int setCurrentTimeMillis(int64_t millis);
    int64_t uptimeMillis();
    int64_t elapsedRealtime();
    int64_t elapsedRealtimeNano();

}; // namespace android

#endif // ANDROID_UTILS_SYSTEMCLOCK_H

