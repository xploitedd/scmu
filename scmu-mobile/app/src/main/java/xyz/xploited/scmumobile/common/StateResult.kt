package xyz.xploited.scmumobile.common

open class StateResult<T> private constructor() {
    companion object {
        fun <T> success(value: T) = Success(value)
        fun <T> error(error: Throwable? = null): StateResult<T> = Error(error)
        fun <T> loading(): StateResult<T> = Loading()
    }

    data class Success<T>(val value: T) : StateResult<T>()
    data class Error<T>(val error: Throwable? = null) : StateResult<T>()
    class Loading<T> : StateResult<T>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading
}