cmake_minimum_required(VERSION 3.22.1)
project(draw_remote_codec LANGUAGES CXX)
add_library(draw_remote_codec SHARED fpzip_jni.cpp
    fpzip/src/read.cpp fpzip/src/rcdecoder.cpp fpzip/src/rcqsmodel.cpp fpzip/src/error.cpp)
target_include_directories(draw_remote_codec PRIVATE fpzip/include fpzip/src)
target_compile_features(draw_remote_codec PRIVATE cxx_std_17)
target_compile_definitions(draw_remote_codec PRIVATE FPZIP_FP=1 FPZIP_BLOCK_SIZE=1)
target_compile_options(draw_remote_codec PRIVATE -fno-fast-math -ffp-contract=off)
