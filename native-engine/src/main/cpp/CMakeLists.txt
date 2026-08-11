cmake_minimum_required(VERSION 3.22.1)
project("chessengine")

add_library(chessengine SHARED engine.cpp)

target_compile_features(chessengine PRIVATE cxx_std_17)
target_compile_options(chessengine PRIVATE -O2 -Wall)

# Simple API surface kept free of STL so the JNI layer stays tiny.
find_library(log-lib log)
target_link_libraries(chessengine ${log-lib})

target_include_directories(chessengine PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})