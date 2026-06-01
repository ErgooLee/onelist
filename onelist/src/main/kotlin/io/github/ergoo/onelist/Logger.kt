package io.github.ergoo.onelist

import android.util.Log

private const val TAG = "ONE-LIST"

internal class Log {
    companion object {
        fun d(subTag: String, msg: String) {
            logger.d("$TAG-$subTag", msg)
        }

        fun i(subTag: String, msg: String) {
            logger.i("$TAG-$subTag", msg)
        }

        fun w(subTag: String, msg: String) {
            logger.w("$TAG-$subTag", msg)
        }

        fun e(subTag: String, msg: String, throwable: Throwable? = null) {
            logger.e("$TAG-$subTag", msg, throwable)
        }

    }
}

var logger = object : Logger {

    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    override fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
        Log.e(tag, msg, throwable)
    }

}

interface Logger {

    fun d(tag: String, msg: String) {}

    fun i(tag: String, msg: String) {}

    fun w(tag: String, msg: String) {}

    fun e(tag: String, msg: String, throwable: Throwable? = null) {}
}