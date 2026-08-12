// Stockfish engine wrapper for TRX-CHESS
// Provides a simple C API for JNI access built on the modern Stockfish Engine class.

#include "engine.h"

#include "stockfish/src/engine.h"
#include "stockfish/src/search.h"
#include "stockfish/src/types.h"
#include "stockfish/src/position.h"
#include "stockfish/src/misc.h"

#include <algorithm>
#include <cstring>
#include <mutex>
#include <string>
#include <string_view>

using namespace Stockfish;

namespace {

constexpr int MATE_SCORE   = 100000;
constexpr int SEARCH_MS    = 400;

std::mutex engineMutex;
Engine*    gEngine  = nullptr;
bool       gInitOk  = false;
int        gDepth   = 18;
std::string gLastFen;
std::string gLastBestmove;
int        gLastEval = 0;
int        gLastDepth = 0;
std::string gLastPV;
long long  gLastNodes = 0;
long long  gLastTime = 0;

int scoreToCentipawns(const Score& s) {
    if (s.is<Score::Mate>()) {
        Score::Mate mate = s.get<Score::Mate>();
        int m = mate.plies > 0 ? mate.plies : -mate.plies;
        return (mate.plies > 0 ? 1 : -1) * (MATE_SCORE - m);
    }
    if (s.is<Score::Tablebase>()) {
        Score::Tablebase tb = s.get<Score::Tablebase>();
        return (tb.win ? 1 : -1) * (MATE_SCORE - 50);
    }
    return s.get<Score::InternalUnits>().value;
}

void captureNoMoves(const Engine::InfoShort& info) {
    gLastEval = scoreToCentipawns(info.score);
}

void captureBestmove(std::string_view bestmove, std::string_view /*ponder*/) {
    gLastBestmove = std::string(bestmove);
}

void captureInfo(const Engine::InfoFull& info) {
    gLastTime  = static_cast<long long>(info.timeMs);
    gLastNodes = static_cast<long long>(info.nodes);
    gLastDepth = info.depth;
    gLastEval  = scoreToCentipawns(info.score);
    gLastPV    = std::string(info.pv);
}

// Runs a search on the currently loaded position and captures best move +
// eval + PV. Caller must hold engineMutex. Returns 0 on success.
int runSearch() {
    gLastBestmove.clear();
    gLastEval = 0;
    gLastPV.clear();
    gLastNodes = 0;
    gLastTime = 0;

    Search::LimitsType limits;
    limits.depth    = gDepth;
    limits.movetime = SEARCH_MS;

    try {
        gEngine->go(limits);
        gEngine->wait_for_search_finished();
    } catch (...) {
        return -1;
    }
    return 0;
}

char sideOfFen(const std::string& fen) {
    auto sp = fen.find(' ');
    return sp == std::string::npos ? 'w' : fen[sp + 1];
}

}  // namespace

extern "C" {

int chess_engine_version(void) { return ENGINE_BINDING_VERSION; }

bool chess_engine_init(void) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if (gInitOk) return true;
    try {
        gEngine = new Engine();
        gEngine->set_on_update_no_moves(captureNoMoves);
        gEngine->set_on_bestmove(captureBestmove);
        gEngine->set_on_update_full(captureInfo);
        gInitOk = true;
    } catch (...) {
        gInitOk = false;
    }
    return gInitOk;
}

int chess_engine_load(const char* fen) {
    if (!fen) return -1;
    std::lock_guard<std::mutex> lock(engineMutex);
    if (!gInitOk) return -1;
    try {
        auto err = gEngine->set_position(fen, {});
        if (err.has_value()) return -1;
        gLastFen  = fen;
        gLastEval = 0;
        gLastPV.clear();
        return 0;
    } catch (...) {
        return -1;
    }
}

int chess_engine_best_move(char* out, int cap) {
    if (!out || cap <= 0) return 0;
    std::lock_guard<std::mutex> lock(engineMutex);
    if (!gInitOk) return 0;
    if (runSearch() != 0) {
        out[0] = '\0';
        return 0;
    }
    if (gLastBestmove.empty()) {
        out[0] = '\0';
        return 0;
    }
    int n = std::min(cap - 1, static_cast<int>(gLastBestmove.size()));
    std::memcpy(out, gLastBestmove.data(), static_cast<size_t>(n));
    out[n] = '\0';
    return n;
}

int chess_engine_eval(void) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if (!gInitOk) return 0;
    if (runSearch() != 0) return 0;
    int score = gLastEval;
    if (sideOfFen(gLastFen) == 'b') score = -score;
    return score;
}

void chess_engine_set_depth(int depth) {
    if (depth > 0 && depth <= 64) gDepth = depth;
}

void chess_engine_get_pv(char* out, int cap, int* scores, int max_moves) {
    std::lock_guard<std::mutex> lock(engineMutex);
    if (!out || cap <= 0) return;
    if (gLastPV.empty()) {
        out[0] = '\0';
        return;
    }
    int n = std::min(cap - 1, static_cast<int>(gLastPV.size()));
    std::memcpy(out, gLastPV.data(), static_cast<size_t>(n));
    out[n] = '\0';
    if (scores) {
        for (int i = 0; i < max_moves; ++i) scores[i] = i == 0 ? gLastEval : 0;
    }
}

int chess_engine_analyze(const char* fen, int depth, char* best_out, int* eval_out,
                         char* pv_out, int pv_cap) {
    if (!fen || !best_out || !eval_out || !pv_out) return -1;
    std::lock_guard<std::mutex> lock(engineMutex);
    if (!gInitOk) return -1;
    // Direct position setting without recursive locking
    try {
        auto err = gEngine->set_position(fen, {});
        if (err.has_value()) return -1;
        gLastFen = fen;
        gLastEval = 0;
        gLastPV.clear();
    } catch (...) {
        return -1;
    }
    int oldDepth = gDepth;
    if (depth > 0) gDepth = depth;
    int rc = runSearch();
    gDepth = oldDepth;
    if (rc != 0) return -1;

    best_out[0] = '\0';
    if (!gLastBestmove.empty()) {
        int n = std::min(15, static_cast<int>(gLastBestmove.size()));
        std::memcpy(best_out, gLastBestmove.data(), static_cast<size_t>(n));
        best_out[n] = '\0';
    }
    *eval_out = gLastEval;
    if (pv_cap > 0) {
        pv_out[0] = '\0';
        if (!gLastPV.empty()) {
            int n = std::min(pv_cap - 1, static_cast<int>(gLastPV.size()));
            std::memcpy(pv_out, gLastPV.data(), static_cast<size_t>(n));
            pv_out[n] = '\0';
        }
    }
    return 0;
}

long long chess_engine_nodes(void) { return gLastNodes; }

long long chess_engine_time(void) { return gLastTime; }

}  // extern "C"