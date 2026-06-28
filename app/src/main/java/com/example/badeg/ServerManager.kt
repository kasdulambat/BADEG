package com.example.badeg

object ServerManager {
    init {
        System.loadLibrary("native-lib")
    }

    external fun startServer(docRoot: String)
    external fun stopServer()
    external fun nativeLog(msg: String)
}
