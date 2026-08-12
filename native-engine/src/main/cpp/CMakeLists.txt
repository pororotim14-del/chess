cmake_minimum_required(VERSION 3.22.1)
project("chessengine")

# Stockfish requires C++17 and exceptions
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(CMAKE_CXX_COMPILER_ID MATCHES "GNU|Clang")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -pipe -fexceptions")
endif()

# Stockfish source files - only actual .cpp files (no layers/*.cpp as they're headers)
# Exclude universal (arch-specific code), benchmark.cpp, tune.cpp
set(STOCKFISH_SOURCES
    # Core
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/attacks.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/benchmark.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/bitboard.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/engine.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/evaluate.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/memory.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/misc.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/movegen.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/movepick.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/position.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/score.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/search.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/thread.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/timeman.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/tt.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/uci.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/ucioption.cpp
    # NNUE
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue/network.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue/nnue_accumulator.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue/nnue_misc.cpp
    # NNUE features
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue/features/half_ka_v2_hm.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue/features/full_threats.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue/features/pp_3wide.cpp
    # Syzygy
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/syzygy/tbprobe.cpp
)

# Our JNI bridge and engine wrapper
add_library(chessengine SHARED
    engine.cpp
    jni_bridge.cpp
    ${STOCKFISH_SOURCES}
)

# Include directories for Stockfish
target_include_directories(chessengine PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/nnue/features
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish/src/syzygy
)

# Stockfish compile definitions (NNUE embedded, single-threaded phone build)
target_compile_definitions(chessengine PRIVATE
    NNUE_EMBEDDED=ON
    USE_NNU_E=ON
    NO_PREFETCH=ON
    NO_AVX2=ON
    NO_SSE41=ON
    NO_POPCNT=ON
)

target_compile_features(chessengine PRIVATE cxx_std_17)
target_compile_options(chessengine PRIVATE -O3 -Wall -Wextra -fexceptions)

find_library(log-lib log)
target_link_libraries(chessengine ${log-lib} android)