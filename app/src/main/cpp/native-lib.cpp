#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <android/bitmap.h>
#include <cstring>
#include <unistd.h>
#include <cstdlib>

extern "C" {
JNIEXPORT jobject JNICALL Java_com_example_zipfileinmemoryjni_JniByteArrayHolder_allocate(
        JNIEnv *env, jobject obj, jlong size) {
    jbyteArray *array = (jbyteArray *) malloc(size);
    if (array == NULL) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI Routine", "Failed to allocate memory\n");
        exit(1);
    }
    memset(array, 0xFF, size);
    return env->NewDirectByteBuffer(array, size);
}

JNIEXPORT void JNICALL Java_com_example_zipfileinmemoryjni_JniByteArrayHolder_freeBuffer(
        JNIEnv *env, jobject obj, jobject buffer) {
    void *bufferLoc = env->GetDirectBufferAddress(buffer);
    __android_log_print(ANDROID_LOG_DEBUG, "Applog", "Freeing buffer\n");
    free(bufferLoc);
    __android_log_print(ANDROID_LOG_DEBUG, "Applog", "Buffer freed\n");
}
}