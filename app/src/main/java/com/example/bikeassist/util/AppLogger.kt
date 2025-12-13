package com.example.bikeassist.util

import android.util.Log

object AppLogger {
    private const val TAG = "BikeAssist"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(throwable: Throwable? = null, message: String) {
        Log.e(TAG, message, throwable)
    }
}
