#ifndef CHESS_ENGINE_ENGINE_H
#define CHESS_ENGINE_ENGINE_H

#define ENGINE_BINDING_VERSION 4

#ifdef __cplusplus
extern "C" {
#endif

int chess_engine_version(void);

bool chess_engine_init(void);

int chess_engine_load(const char* fen);

int chess_engine_best_move(char* out, int cap);

int chess_engine_eval(void);

void chess_engine_set_depth(int depth);

void chess_engine_get_pv(char* out, int cap, int* scores, int max_moves);

int chess_engine_analyze(const char* fen, int depth, char* best_out, int* eval_out, char* pv_out, int pv_cap);

long long chess_engine_nodes(void);

long long chess_engine_time(void);

#ifdef __cplusplus
}
#endif

#endif