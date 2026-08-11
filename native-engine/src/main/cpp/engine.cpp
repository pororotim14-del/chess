#include <jni.h>
#include <string.h>

#include "engine.h"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_bindingVersion(
    JNIEnv* /*env*/, jobject /*thiz*/) {
  return ENGINE_BINDING_VERSION;
}

JNIEXPORT jint JNICALL
Java_com_chessassistant_nativeengine_NativeEngine_evalSummary(
    JNIEnv* /*env*/, jobject /*thiz*/, jstring /*fen*/) {
  // Placeholder; returns 0 (equal) until the native search lands.
  return 0;
}

#ifdef __cplusplus
}
#endif