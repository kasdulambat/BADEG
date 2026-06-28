#include <jni.h>
#include <pthread.h>
#include <string.h>
#include "mongoose.h"

static struct mg_mgr mgr;
static char web_root_path[256];

static void fn(struct mg_connection *c, int ev, void *ev_data, void *fn_data) {
    if (ev == MG_EV_HTTP_MSG) {
        struct mg_http_serve_opts opts = {.root_dir = web_root_path};
        mg_http_serve_dir(c, ev_data, &opts);
    }
}

void* mongoose_thread(void *arg) {
    mg_mgr_init(&mgr);
    mg_http_listen(&mgr, "http://0.0.0.0:8080", fn, NULL);
    for (;;) mg_mgr_poll(&mgr, 1000);
    mg_mgr_free(&mgr);
    return NULL;
}

JNIEXPORT void JNICALL
Java_com_example_badeg_MainActivity_startServer(JNIEnv *env, jobject thiz, jstring doc_root) {
    const char *path = (*env)->GetStringUTFChars(env, doc_root, 0);
    strcpy(web_root_path, path);
    (*env)->ReleaseStringUTFChars(env, doc_root, path);

    pthread_t thread_id;
    pthread_create(&thread_id, NULL, mongoose_thread, NULL);
}
