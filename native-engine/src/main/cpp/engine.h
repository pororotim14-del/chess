#ifndef CHESS_ENGINE_ENGINE_H
#define CHESS_ENGINE_ENGINE_H

#define ENGINE_BINDING_VERSION 1

#ifdef __cplusplus
extern "C" {
#endif

/* UTF-8 updates a position from FEN by reference, returns 0 on success. */
int chess_engine_update(const char* fen);

/* Returns 0 if position is equal, >0 if white is better, <0 otherwise. */
int chess_engine_eval(void);

#ifdef __cplusplus
}
#endif

#endif  // CHESS_ENGINE_ENGINE_H