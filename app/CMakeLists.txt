cmake_minimum_required(VERSION 3.4.1)

# Add the function library
file(GLOB DUMPDEX_FILES "src/main/cpp/dumpdex/*.cpp")
file(GLOB RAR_FILES "src/main/cpp/unrar/*.cpp")
file(GLOB ASTYLE_FILES "src/main/cpp/astyle/*.cpp")
file(GLOB ZIPA_FILES "src/main/cpp/zipalign/*.cpp"
        "src/main/cpp/zipalign/android/native/src/utils/*.cpp")

add_library(function SHARED src/main/cpp/tracepath.c src/main/cpp/miniTelnet.c src/main/cpp/Provider.c src/main/cpp/ELFHash.cpp src/main/cpp/mainfunction.cpp src/main/cpp/rarext.cpp src/main/cpp/oat2dex.cpp src/main/cpp/parse_elf.cpp
        src/main/cpp/odex2dex.cpp src/main/cpp/zipa.cpp src/main/cpp/dumpdex_main.cpp src/main/cpp/utils.cpp ${DUMPDEX_FILES} ${RAR_FILES} ${ZIPA_FILES} ${ASTYLE_FILES})

target_include_directories(function PRIVATE src/main/cpp/zipalign
        src/main/cpp/zipalign/android/base/include
        src/main/cpp/zipalign/android/core/include
        src/main/cpp/zipalign/android/native/include
        src/main/cpp/dumpdex)

target_link_libraries(function PRIVATE log z)
target_compile_options(function PRIVATE -DASTYLE_JNI -DSILENT -DRARDLL -fexceptions -Os -frtti -ffunction-sections -fdata-sections -fvisibility=hidden -w -Wl,--gc-sections)
set_target_properties(function PROPERTIES CXX_STANDARD 11)

# Add the FileHelper library
add_library(FileHelper SHARED src/main/cpp/android_os_FileHelper.cpp)
set_target_properties(FileHelper PROPERTIES CXX_STANDARD 11)