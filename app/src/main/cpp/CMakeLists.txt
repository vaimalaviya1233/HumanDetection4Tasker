cmake_minimum_required(VERSION 3.22)
project(llamacpp_jni)

# llama.cpp Android build flags
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS ON CACHE BOOL "" FORCE)

# GPU OpenCL (Adreno) - disabled by default, enable with -DGGML_OPENCL=ON
# set(GGML_OPENCL ON CACHE BOOL "" FORCE)

# Include llama.cpp as subdirectory
add_subdirectory(${CMAKE_SOURCE_DIR}/../../../../llama.cpp
                 ${CMAKE_BINARY_DIR}/llama.cpp)

# JNI bridge library
add_library(llamacpp_jni SHARED llamacpp_jni.cpp)

target_include_directories(llamacpp_jni PRIVATE
    ${CMAKE_SOURCE_DIR}/../../../../llama.cpp/include
    ${CMAKE_SOURCE_DIR}/../../../../llama.cpp/tools/mtmd
    ${CMAKE_SOURCE_DIR}/../../../../llama.cpp/common
)

target_link_libraries(llamacpp_jni
    llama
    mtmd
    common
    ggml
    log
)
