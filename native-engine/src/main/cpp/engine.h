#ifndef CHESS_ENGINE_ENGINE_H
#define CHESS_ENGINE_ENGINE_H

#define ENGINE_BINDING_VERSION 2

#ifdef __cplusplus
extern "C" {
#endif

/* Returns ENGINE_BINDING_VERSION. */
int chess_engine_version(void);

/* Parses a FEN into the internal position. Returns 0 on success. */
int chess_engine_load(const char* fen);

/*
 * Searches the loaded position and copies the best move in UCI notation
 * (e.g. "e2e4", "a7a8q") into `out` (which holds `cap` bytes).
 * Returns the number of bytes written (0 when there is no legal move).
 */
int chess_engine_best_move(char* out, int cap);

/* Static evaluation of the loaded position in centipawns from White's view. */
int chess_engine_eval(void);

#ifdef __cplusplus
}
#endif

#endif  // CHESS_ENGINE_ENGINE_H