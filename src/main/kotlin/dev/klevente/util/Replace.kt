package dev.klevente.util

inline fun <T> Collection<T>.replaceIndex(index: Int, replacer: (T) -> T): Collection<T> {
    return mapIndexed { i, t ->
        if (index == i) {
            replacer(t)
        } else {
            t
        }
    }
}

fun String.removePrefix() = substring(1)