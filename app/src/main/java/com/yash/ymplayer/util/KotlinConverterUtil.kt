@file:JvmName("KotlinConverterUtil")
package com.yash.ymplayer.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow

public class KotlinConverterUtil {
    companion object {
        fun <T : Any> toFlowable(flow: Flow<T>): LiveData<T> {
            return flow.asLiveData()
        }
    }

}