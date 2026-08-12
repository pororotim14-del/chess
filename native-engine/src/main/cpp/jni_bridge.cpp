// JNI bridge: exposes the Stockfish engine API to Kotlin.
#include <jni.h>
#include <cstring>

#include "engine.h"

extern "C" {

JNIEXPORT jint JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_bindingVersion(JNIEnv* env,
                                                                  jclass clazz) {
  return chess_engine_version();
}

JNIEXPORT jint JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_evalSummary(JNIEnv* env,
                                                              jclass clazz,
                                                              jstring fen) {
  chess_engine_init();
  const char* fenChars = env->GetStringUTFChars(fen, nullptr);
  if (!fenChars) return 0;
  int ok = chess_engine_load(fenChars);
  env->ReleaseStringUTFChars(fen, fenChars);
  if (ok != 0) return 0;
  return chess_engine_eval();
}

JNIEXPORT jstring JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_bestMove(JNIEnv* env,
                                                            jclass clazz,
                                                            jstring fen) {
  chess_engine_init();
  const char* fenChars = env->GetStringUTFChars(fen, nullptr);
  if (!fenChars) return env->NewStringUTF("");
  int ok = chess_engine_load(fenChars);
  env->ReleaseStringUTFChars(fen, fenChars);
  if (ok != 0) return env->NewStringUTF("");

  char out[16];
  int n = chess_engine_best_move(out, static_cast<int>(sizeof(out)));
  if (n <= 0) return env->NewStringUTF("");
  return env->NewStringUTF(out);
}

JNIEXPORT void JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_setSearchDepth(JNIEnv* env,
                                                                  jclass clazz,
                                                                  jint depth) {
  chess_engine_init();
  chess_engine_set_depth(depth);
}

JNIEXPORT jstring JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_getPrincipalVariation(JNIEnv* env,
                                                                         jclass clazz) {
  chess_engine_init();
  char pv[256];
  chess_engine_get_pv(pv, sizeof(pv), nullptr, 0);
  return env->NewStringUTF(pv);
}

JNIEXPORT jobject JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_analyzePosition(JNIEnv* env,
                                                                   jclass clazz,
                                                                   jstring fen,
                                                                   jint depth) {
  chess_engine_init();
  const char* fenChars = env->GetStringUTFChars(fen, nullptr);
  if (!fenChars) return nullptr;

  char bestMove[16];
  int eval = 0;
  char pv[512];

  int result = chess_engine_analyze(fenChars, depth, bestMove, &eval, pv, sizeof(pv));
  long long nodes = chess_engine_nodes();
  long long timeMs = chess_engine_time();

  env->ReleaseStringUTFChars(fen, fenChars);

  if (result != 0) return nullptr;

  jclass resultClass = env->FindClass("com/chessassistant/nativeengine/NativeEngine$AnalysisResult");
  if (!resultClass) return nullptr;

  jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(Ljava/lang/String;ILjava/lang/String;IJJ)V");
  if (!constructor) return nullptr;

  jstring bestMoveStr = env->NewStringUTF(bestMove);
  jstring pvStr = env->NewStringUTF(pv);

  return env->NewObject(resultClass, constructor,
                        bestMoveStr, eval, pvStr, depth,
                        static_cast<jlong>(nodes), static_cast<jlong>(timeMs));
}

JNIEXPORT jboolean JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_verifyEngineIntegrity(JNIEnv* env,
                                                                         jclass clazz) {
  chess_engine_init();
  const char* testFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
  char bestMove[16];
  int eval = 0;
  char pv[256];
  int result = chess_engine_analyze(testFen, 1, bestMove, &eval, pv, sizeof(pv));
  return (result == 0 && bestMove[0] != '\0' && chess_engine_version() >= 3)
             ? JNI_TRUE
             : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_getEngineFingerprint(JNIEnv* env,
                                                                        jclass clazz) {
  chess_engine_init();
  const char* fingerprint = "Stockfish-master-5062aee-TRX-CHESS-Secure";
  return env->NewStringUTF(fingerprint);
}

}  // extern "C"