package com.zdy.android.library.adapter.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import java.lang.reflect.Type
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 适配Call<ResponseBody>或Call<Bean>
 */
class BodyFlowCallAdapter<R>(private val responseBodyType: R) : CallAdapter<R, Flow<R>> {

    override fun responseType(): Type = responseBodyType as Type

    override fun adapt(call: Call<R>): Flow<R> = flow {
        suspendCancellableCoroutine<R> { continuation ->
            // 协程取消时调用call.cancel取消请求
            continuation.invokeOnCancellation {
                call.cancel()
            }
            try {
                // 执行请求
                val response = call.execute()
                if (response.isSuccessful) {
                    // 返回的请求码在[200,300)认定网络请求成功，恢复运行并返回数据
                    continuation.resume(response.body()!!)
                } else {
                    // 认定网络请求不成功，抛出异常
                    continuation.resumeWithException(HttpException(response))
                }
            } catch (e: Exception) {
                // 处理捕获的其他异常
                continuation.resumeWithException(e)
            }
        }.let { responseBody ->
            emit(responseBody)
        }
    }
}