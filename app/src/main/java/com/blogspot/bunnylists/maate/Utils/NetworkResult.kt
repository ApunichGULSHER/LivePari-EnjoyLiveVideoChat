package com.blogspot.bunnylists.maate.Utils

import android.net.Uri

sealed class NetworkResult<T>(val data: Any? = null, val massage: String? = null) {

    class Success<T>(massage: String? = null, data: Any? = null) : NetworkResult<T>(data, massage)
    class Error<T>(massage: String? = null, data: Any? = null) : NetworkResult<T>(data, massage)
    class Loading<T> : NetworkResult<T>()
}