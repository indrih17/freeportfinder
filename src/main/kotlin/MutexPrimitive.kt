import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MutexPrimitive<T>(private var elem: T) {
    internal val mutex = Mutex()

    suspend fun set(value: T) = mutex.withLock {
        elem = value
    }

    suspend fun get(): T = mutex.withLock {
        elem
    }

    internal fun setWithoutLock(value: T) {
        elem = value
    }

    internal fun getWithOutLock(): T =
        elem
}

suspend fun MutexPrimitive<Int>.increment() = mutex.withLock {
    setWithoutLock(value = getWithOutLock() + 1)
}