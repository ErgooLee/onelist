package io.github.ergoo.onelist.demo

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LifecycleLogObserver(
    private val tag: String,
    private val ownerName: String,
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        Log.d(tag, "$ownerName onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(tag, "$ownerName onStart")
    }

    override fun onResume(owner: LifecycleOwner) {
        Log.d(tag, "$ownerName onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d(tag, "$ownerName onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(tag, "$ownerName onStop")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(tag, "$ownerName onDestroy")
    }
}

