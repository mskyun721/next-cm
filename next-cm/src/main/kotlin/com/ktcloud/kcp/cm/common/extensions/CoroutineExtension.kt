package com.ktcloud.kcp.cm.common.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun <T1 : Any, T2 : Any> asyncAndAwait(
    block1: suspend CoroutineScope.() -> T1,
    block2: suspend CoroutineScope.() -> T2,
): Pair<T1, T2> = coroutineScope {
    val d1 = async { block1() }
    val d2 = async { block2() }
    d1.await() to d2.await()
}

suspend fun <T1 : Any, T2 : Any, T3 : Any> asyncAndAwait(
    block1: suspend CoroutineScope.() -> T1,
    block2: suspend CoroutineScope.() -> T2,
    block3: suspend CoroutineScope.() -> T3,
): Triple<T1, T2, T3> = coroutineScope {
    val d1 = async { block1() }
    val d2 = async { block2() }
    val d3 = async { block3() }
    Triple(d1.await(), d2.await(), d3.await())
}

suspend fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any> asyncAndAwait(
    block1: suspend CoroutineScope.() -> T1,
    block2: suspend CoroutineScope.() -> T2,
    block3: suspend CoroutineScope.() -> T3,
    block4: suspend CoroutineScope.() -> T4,
): Tuple4<T1, T2, T3, T4> = coroutineScope {
    val d1 = async { block1() }
    val d2 = async { block2() }
    val d3 = async { block3() }
    val d4 = async { block4() }
    Tuple4(d1.await(), d2.await(), d3.await(), d4.await())
}

suspend fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any> asyncAndAwait(
    block1: suspend CoroutineScope.() -> T1,
    block2: suspend CoroutineScope.() -> T2,
    block3: suspend CoroutineScope.() -> T3,
    block4: suspend CoroutineScope.() -> T4,
    block5: suspend CoroutineScope.() -> T5,
): Tuple5<T1, T2, T3, T4, T5> = coroutineScope {
    val d1 = async { block1() }
    val d2 = async { block2() }
    val d3 = async { block3() }
    val d4 = async { block4() }
    val d5 = async { block5() }
    Tuple5(d1.await(), d2.await(), d3.await(), d4.await(), d5.await())
}

suspend fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any> asyncAndAwait(
    block1: suspend CoroutineScope.() -> T1,
    block2: suspend CoroutineScope.() -> T2,
    block3: suspend CoroutineScope.() -> T3,
    block4: suspend CoroutineScope.() -> T4,
    block5: suspend CoroutineScope.() -> T5,
    block6: suspend CoroutineScope.() -> T6,
): Tuple6<T1, T2, T3, T4, T5, T6> = coroutineScope {
    val d1 = async { block1() }
    val d2 = async { block2() }
    val d3 = async { block3() }
    val d4 = async { block4() }
    val d5 = async { block5() }
    val d6 = async { block6() }
    Tuple6(d1.await(), d2.await(), d3.await(), d4.await(), d5.await(), d6.await())
}

suspend fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any> asyncAndAwait(
    block1: suspend CoroutineScope.() -> T1,
    block2: suspend CoroutineScope.() -> T2,
    block3: suspend CoroutineScope.() -> T3,
    block4: suspend CoroutineScope.() -> T4,
    block5: suspend CoroutineScope.() -> T5,
    block6: suspend CoroutineScope.() -> T6,
    block7: suspend CoroutineScope.() -> T7,
): Tuple7<T1, T2, T3, T4, T5, T6, T7> = coroutineScope {
    val d1 = async { block1() }
    val d2 = async { block2() }
    val d3 = async { block3() }
    val d4 = async { block4() }
    val d5 = async { block5() }
    val d6 = async { block6() }
    val d7 = async { block7() }
    Tuple7(d1.await(), d2.await(), d3.await(), d4.await(), d5.await(), d6.await(), d7.await())
}

suspend fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any, T7 : Any, T8 : Any> asyncAndAwait(
    block1: suspend CoroutineScope.() -> T1,
    block2: suspend CoroutineScope.() -> T2,
    block3: suspend CoroutineScope.() -> T3,
    block4: suspend CoroutineScope.() -> T4,
    block5: suspend CoroutineScope.() -> T5,
    block6: suspend CoroutineScope.() -> T6,
    block7: suspend CoroutineScope.() -> T7,
    block8: suspend CoroutineScope.() -> T8,
): Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> = coroutineScope {
    val d1 = async { block1() }
    val d2 = async { block2() }
    val d3 = async { block3() }
    val d4 = async { block4() }
    val d5 = async { block5() }
    val d6 = async { block6() }
    val d7 = async { block7() }
    val d8 = async { block8() }
    Tuple8(d1.await(), d2.await(), d3.await(), d4.await(), d5.await(), d6.await(), d7.await(), d8.await())
}
