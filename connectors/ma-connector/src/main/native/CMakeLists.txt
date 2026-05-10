cmake_minimum_required(VERSION 3.20)
project(RevPiReader LANGUAGES CXX)

find_package(JNI REQUIRED)

add_library(RevPiReader SHARED RevPiReader.cpp)

target_compile_features(RevPiReader PRIVATE cxx_std_17)

target_include_directories(RevPiReader PRIVATE
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(RevPiReader PRIVATE -Wall -Wextra -Wpedantic)
