// JNI bridge: exposes the plain C engine API to Kotlin.
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

}  // extern "C"
