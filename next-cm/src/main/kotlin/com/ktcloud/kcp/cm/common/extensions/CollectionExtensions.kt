package com.ktcloud.kcp.cm.common.extensions

/**
 * @author MooHee Lee
 */
fun <T, A, B> Iterable<T>.unzipBy(first: (T) -> A, second: (T) -> B): Pair<List<A>, List<B>> {
    val firstValues = mutableListOf<A>()
    val secondValues = mutableListOf<B>()

    for (element in this) {
        firstValues += first(element)
        secondValues += second(element)
    }

    return firstValues to secondValues
}

fun <T, A, B, C> Iterable<T>.unzipBy(
    first: (T) -> A,
    second: (T) -> B,
    third: (T) -> C,
): Triple<List<A>, List<B>, List<C>> {
    val firstValues = mutableListOf<A>()
    val secondValues = mutableListOf<B>()
    val thirdValues = mutableListOf<C>()

    for (element in this) {
        firstValues += first(element)
        secondValues += second(element)
        thirdValues += third(element)
    }

    return Triple(firstValues, secondValues, thirdValues)
}

fun <T, A, B, C, D> Iterable<T>.unzipBy(
    first: (T) -> A,
    second: (T) -> B,
    third: (T) -> C,
    fourth: (T) -> D,
): Tuple4<List<A>, List<B>, List<C>, List<D>> {
    val firstValues = mutableListOf<A>()
    val secondValues = mutableListOf<B>()
    val thirdValues = mutableListOf<C>()
    val fourthValues = mutableListOf<D>()

    for (element in this) {
        firstValues += first(element)
        secondValues += second(element)
        thirdValues += third(element)
        fourthValues += fourth(element)
    }

    return Tuple4(firstValues, secondValues, thirdValues, fourthValues)
}
