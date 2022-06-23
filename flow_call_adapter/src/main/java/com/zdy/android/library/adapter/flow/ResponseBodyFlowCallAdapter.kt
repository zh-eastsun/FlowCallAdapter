package com.zdy.android.library.adapter.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import retrofit2.Response
import java.lang.reflect.Type
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 适配Call<Response<ResponseBody>>或Call<Response<Bean>>
 */
class ResponseBodyFlowCallAdapter<R>(private val responseBodyType: R) :
    CallAdapter<R, Flow<Response<R>>> {
    override fun responseType(): Type = responseBodyType as Type

    override fun adapt(call: Call<R>): Flow<Response<R>> = flow {
        suspendCancellableCoroutine<Response<R>> { continuation ->
            // 但协程被取消时调用这个方法
            continuation.invokeOnCancellation {
                call.cancel()
            }
            try {
                // 执行网络请求
                val response = call.execute()
                if (response.isSuccessful) {
                    continuation.resume(response)
                } else {
                    // 如果不成功就抛出异常
                    continuation.resumeWithException(HttpException(response))
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }.let { response ->
            emit(response)
        }
    }
}