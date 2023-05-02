@file:JvmName("KotlinConverterUtil")
package com.yash.ymplayer.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow

public class KotlinConverterUtil {
    companion object {
        @JvmStatic
        fun <T : Any> toLiveData(flow: Flow<T>): LiveData<T> {
            return flow.asLiveData()
        }
    }

}