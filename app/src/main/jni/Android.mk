LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libiconv
LIBICONV := qrcode/libiconv
LOCAL_CFLAGS := -I$(LOCAL_PATH)/$(LIBICONV)
LOCAL_SRC_FILES := $(LIBICONV)/iconv.c
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)  
LOCAL_MODULE := function

QRCODE_FILES = $(wildcard $(LOCAL_PATH)/qrcode/*.c) \
	$(wildcard $(LOCAL_PATH)/qrcode/qrcode/*.c) \
	qrcode/processor/null.c \
	qrcode/video/null.c \
	qrcode/window/null.c \
	$(wildcard $(LOCAL_PATH)/qrcode/decoder/*.c)

RAR_FILES := $(wildcard $(LOCAL_PATH)/unrar/*.cpp)

DUMPDEX_FILES = $(wildcard $(LOCAL_PATH)/dumpdex/*.cpp)

ASTYLE_FILES = $(wildcard $(LOCAL_PATH)/astyle/*.cpp)

ZIPA_FILES = $(wildcard $(LOCAL_PATH)/zipalign/*.cpp) $(wildcard $(LOCAL_PATH)/zipalign/android/native/src/utils/*.cpp)

LOCAL_SRC_FILES := tracepath.c miniTelnet.c Provider.c ELFHash.cpp mainfunction.cpp zlibmgr.cpp rarext.cpp oat2dex.cpp parse_elf.cpp \
		odex2dex.cpp zipa.cpp dumpdex_main.cpp $(QRCODE_FILES) $(DUMPDEX_FILES) $(RAR_FILES) $(ZIPA_FILES) $(ASTYLE_FILES)

LOCAL_LDLIBS := -llog -lz

LOCAL_C_INCLUDES += $(LOCAL_PATH)/zipalign \
$(LOCAL_PATH)/zipalign/android/base/include \
$(LOCAL_PATH)/zipalign/android/core/include \
$(LOCAL_PATH)/zipalign/android/native/include \
$(LOCAL_PATH)/qrcode \
$(LOCAL_PATH)/qrcode/libiconv \
$(LOCAL_PATH)/dumpdex

LOCAL_CFLAGS := -DASTYLE_JNI -DSILENT -DRARDLL -I$(LOCAL_PATH)/rar -fexceptions -Os \
    -ffunction-sections -fdata-sections -fvisibility=hidden \
    -w -Wl,--gc-sections
LOCAL_STATIC_LIBRARIES := libiconv
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := FileHelper
LOCAL_SRC_FILES := android_os_FileHelper.cpp
include $(BUILD_SHARED_LIBRARY)
