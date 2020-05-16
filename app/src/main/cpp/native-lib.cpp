#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <android/bitmap.h>
#include <cstring>
#include <unistd.h>
#include <cstdlib>
#include <limits>
#include <algorithm>
#include <math.h>

extern "C" {
static jbyteArray *_holdBuffer = NULL;
static jobject _directBuffer = NULL;
/*
    This routine is not re-entrant and can handle only one buffer at a time. If a buffer is
    allocated then it must be released before the next one is allocated.
 */
JNIEXPORT
jobject JNICALL Java_com_example_zipfileinmemoryjni_JniByteArrayHolder_allocate(
        JNIEnv *env, jobject obj, jlong size) {
    if (_holdBuffer != NULL || _directBuffer != NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI Routine",
                            "Call to JNI allocate() before freeBuffer()");
        return NULL;
    }

    // Max size for a direct buffer is the max of a jint even though NewDirectByteBuffer takes a
    // long. Clamp max size as follows:
    if (size > SIZE_T_MAX || size > INT_MAX || size <= 0) {
        jlong maxSize = SIZE_T_MAX < INT_MAX ? SIZE_T_MAX : INT_MAX;
        __android_log_print(ANDROID_LOG_ERROR, "JNI Routine",
                            "Native memory allocation request must be >0 and <= %lld but was %lld.\n",
                            maxSize, size);
        return NULL;
    }

    jbyteArray *array = (jbyteArray *) malloc(static_cast<size_t>(size));
    if (array == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI Routine",
                            "Failed to allocate %lld bytes of native memory.\n",
                            size);
        return NULL;
    }

    jobject directBuffer = env->NewDirectByteBuffer(array, size);
    if (directBuffer == NULL) {
        free(array);
        __android_log_print(ANDROID_LOG_ERROR, "JNI Routine",
                            "Failed to create direct buffer of size %lld.\n",
                            size);
        return NULL;
    }
    // memset() is not really needed but we call it here to force Android to count
    // the consumed memory in the stats since it only seems to "count" dirty pages. (?)
    memset(array, 0xFF, static_cast<size_t>(size));
    _holdBuffer = array;

    // Get a global reference to the direct buffer so Java isn't tempted to GC it.
    _directBuffer = env->NewGlobalRef(directBuffer);
    return directBuffer;
}

JNIEXPORT void JNICALL Java_com_example_zipfileinmemoryjni_JniByteArrayHolder_freeBuffer(
        JNIEnv *env, jobject obj, jobject directBuffer) {

    if (_directBuffer == NULL || _holdBuffer == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI Routine",
                            "Attempt to free unallocated buffer.");
        return;
    }

    jbyteArray *bufferLoc = (jbyteArray *) env->GetDirectBufferAddress(directBuffer);
    if (bufferLoc == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI Routine",
                            "Failed to retrieve direct buffer location associated with ByteBuffer.");
        return;
    }

    if (bufferLoc != _holdBuffer) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI Routine",
                            "DirectBuffer does not match that allocated.");
        return;
    }

    // Free the malloc'ed buffer and the global reference. Java can not GC the direct buffer.
    free(bufferLoc);
    env->DeleteGlobalRef(_directBuffer);
    _holdBuffer = NULL;
    _directBuffer = NULL;
}
}