#include <jni.h>
#include <pthread.h>
#include <string.h>
#include <android/log.h>
#include "mongoose.h"

#define TAG "BADEG_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static struct mg_mgr mgr;
static char web_root_path[256];
static int server_running = 0;
static pthread_t thread_id;

// Simple log buffer for the dynamic /api/logs endpoint
#define MAX_LOGS 50
#define MAX_LOG_LINE 128
static char logs_buffer[MAX_LOGS][MAX_LOG_LINE];
static int log_head = 0;
static pthread_mutex_t log_mutex = PTHREAD_MUTEX_INITIALIZER;

void add_to_logs(const char *msg) {
    pthread_mutex_lock(&log_mutex);
    strncpy(logs_buffer[log_head], msg, MAX_LOG_LINE - 1);
    logs_buffer[log_head][MAX_LOG_LINE - 1] = '\0';
    log_head = (log_head + 1) % MAX_LOGS;
    pthread_mutex_unlock(&log_mutex);
}

static void fn(struct mg_connection *c, int ev, void *ev_data) {
    if (ev == MG_EV_HTTP_MSG) {
        struct mg_http_message *hm = (struct mg_http_message *) ev_data;
        if (mg_match(hm->uri, mg_str("/api/logs"), NULL)) {
            struct mg_iobuf b = {NULL, 0, 0, 512};
            mg_iobuf_init(&b, 512, 512);
            mg_iobuf_add(&b, 0, "[", 1);
            pthread_mutex_lock(&log_mutex);
            int count = 0;
            for (int i = 0; i < MAX_LOGS; i++) {
                int idx = (log_head + i) % MAX_LOGS;
                if (logs_buffer[idx][0] != '\0') {
                    if (count > 0) mg_iobuf_add(&b, b.len, ",", 1);
                    mg_xprintf(mg_pfn_iobuf, &b, "%m", MG_ESC(logs_buffer[idx]));
                    count++;
                }
            }
            pthread_mutex_unlock(&log_mutex);
            mg_iobuf_add(&b, b.len, "]", 1);
            mg_http_reply(c, 200, "Content-Type: application/json\r\n", "%.*s", (int) b.len, b.buf);
            mg_iobuf_free(&b);
        } else {
            struct mg_http_serve_opts opts = {.root_dir = web_root_path};
            mg_http_serve_dir(c, hm, &opts);
        }
    }
}

void* mongoose_thread(void *arg) {
    LOGI("Server thread started, root: %s", web_root_path);
    mg_mgr_init(&mgr);
    if (mg_http_listen(&mgr, "http://0.0.0.0:8080", fn, NULL) == NULL) {
        LOGE("Failed to listen on port 8080");
        server_running = 0;
        return NULL;
    }
    add_to_logs("Mongoose Server started on port 8080");
    while (server_running) {
        mg_mgr_poll(&mgr, 100);
    }
    mg_mgr_free(&mgr);
    LOGI("Server thread exiting");
    return NULL;
}

JNIEXPORT void JNICALL
Java_com_example_badeg_ServerManager_startServer(JNIEnv *env, jobject thiz, jstring doc_root) {
    if (server_running) return;

    const char *path = (*env)->GetStringUTFChars(env, doc_root, 0);
    strncpy(web_root_path, path, sizeof(web_root_path) - 1);
    web_root_path[sizeof(web_root_path) - 1] = '\0';
    (*env)->ReleaseStringUTFChars(env, doc_root, path);

    server_running = 1;
    pthread_create(&thread_id, NULL, mongoose_thread, NULL);
}

JNIEXPORT void JNICALL
Java_com_example_badeg_ServerManager_stopServer(JNIEnv *env, jobject thiz) {
    if (!server_running) return;
    server_running = 0;
    pthread_join(thread_id, NULL);
    LOGI("Server stopped");
}

JNIEXPORT void JNICALL
Java_com_example_badeg_ServerManager_nativeLog(JNIEnv *env, jobject thiz, jstring msg) {
    const char *native_msg = (*env)->GetStringUTFChars(env, msg, 0);
    add_to_logs(native_msg);
    LOGI("%s", native_msg);
    (*env)->ReleaseStringUTFChars(env, msg, native_msg);
}
