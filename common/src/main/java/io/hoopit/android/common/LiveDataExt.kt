@file:Suppress("TooManyFunctions")

package io.hoopit.android.common

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

/**
 * Observes the first non-null value and then removes the observer
 */
fun <T> LiveData<T>.observeFirstNotNull(owner: LifecycleOwner, observer: (T) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        if (t != null) {
            removeObserver(requireNotNull(observerWrapper))
            observer.invoke(t)
        }
    }
    observe(owner, observerWrapper)
}

/**
 * Observes the first non-null value and then removes the observer
 */
fun <T> LiveData<T>.observeFirst(owner: LifecycleOwner, observer: (T?) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        removeObserver(requireNotNull(observerWrapper))
        observer.invoke(t)
    }
    observe(owner, observerWrapper)
}

/**
 * Observes the first non-null value and then removes the observer
 */
fun <T> LiveData<T>.observeUntil(
    owner: LifecycleOwner,
    until: (T) -> Boolean,
    observer: (T?) -> Unit
) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        if (until(t)) removeObserver(requireNotNull(observerWrapper))
        observer.invoke(t)
    }
    observe(owner, observerWrapper)
}

/**
 * Observes the first non-null value and then removes the observer
 */
fun <T> LiveData<T>.observeFirstNotNullForever(observer: (T?) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        if (t != null) {
            removeObserver(requireNotNull(observerWrapper))
            observer.invoke(t)
        }
    }
    observeForever(observerWrapper)
}

/**
 * Observes the first value and then removes the observer
 */
fun <T> LiveData<T>.observeFirstForever(observer: (T?) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        observer.invoke(t)
        removeObserver(requireNotNull(observerWrapper))
    }
    observeForever(observerWrapper)
}

/**
 * Extension wrapper for [Transformations.switchMap]
 */
fun <X, Y> LiveData<X>.switchMap(func: (X) -> LiveData<Y>?): LiveData<Y> =
    Transformations.switchMap(this, func)

/**
 * Extension wrapper for [Transformations.map]
 */
fun <X> LiveData<X>.toMutable(): MediatorLiveData<X> = mediatorLiveDataUpdate(this) { it }

/**
 * Extension wrapper for [Transformations.map]
 */
inline fun <X, Y> LiveData<X>.mapUpdate(crossinline func: (X) -> Y): LiveData<Y> {
    val result = MediatorLiveData<Y>()
    result.addSource(this) { x -> result.update(func(x)) }
    return result
}

/**
 * Extension wrapper for [LiveDataReactiveStreams.toPublisher]
 */
//fun <T> LiveData<T>.toPublisher(lifecycleOwner: LifecycleOwner) = LiveDataReactiveStreams.toPublisher(lifecycleOwner, this)

fun <T> MutableLiveData<T>.update(newValue: T) {
    if (this.value != newValue)
        this.value = newValue
}

fun <T> MutableLiveData<T>.postUpdate(newValue: T) {
    if (this.value != newValue)
        this.postValue(newValue)
}

fun <T> liveData(value: T?): LiveData<T> {
    return MutableLiveData<T>().also { it.postValue(value) }
}

fun <T> noLiveData(): LiveData<T> {
    return NoLiveData()
}

fun <T> mutableLiveData(newValue: T): MutableLiveData<T> {
    val data = MutableLiveData<T>()
    data.value = newValue
    return data
}

fun <T> mediatorLiveData(newValue: T?): MediatorLiveData<T> {
    val data = MediatorLiveData<T>()
    data.value = newValue
    return data
}

inline fun <TSOURCE, TOUT> mediatorLiveDataUpdate(
    source: LiveData<TSOURCE>,
    crossinline onChanged: (TSOURCE) -> TOUT
): MediatorLiveData<TOUT> {
    val liveData = MediatorLiveData<TOUT>()
    liveData.addSource(source) {
        liveData.postValue(onChanged(it))
    }
    return liveData
}

inline fun <TSOURCE> LiveData<TSOURCE>.doOnUpdate(
    crossinline onChanged: (TSOURCE) -> Unit
): MediatorLiveData<TSOURCE> {
    val liveData = MediatorLiveData<TSOURCE>()
    liveData.addSource(this) {
        onChanged(it)
        liveData.postValue(it)
    }
    return liveData
}

fun <TSOURCE> MediatorLiveData<TSOURCE>.mergeUpdate(source: LiveData<TSOURCE>) {
    addSource(source, this::postUpdate)
}

fun <TSOURCE, TOUT> mediatorLiveData(
    source: LiveData<TSOURCE>,
    initial: TOUT? = null,
    onChanged: MediatorLiveData<TOUT>.(TSOURCE?) -> Unit
): MediatorLiveData<TOUT> {
    val liveData = MediatorLiveData<TOUT>()
    initial?.let { liveData.postValue(it) }
    liveData.addSource(source) { onChanged(liveData, it) }
    return liveData
}

fun <X, Y, Z> combineLatest(
    leftSrc: LiveData<X>,
    rightSrc: LiveData<Y>,
    onChanged: (X, Y) -> Z
): MediatorLiveData<Z> {
    val liveData = MediatorLiveData<Z>()
    liveData.addSource(leftSrc) { leftVal ->
        leftVal?.let {
            rightSrc.value?.let { liveData.value = onChanged(leftVal, it) }
        }
    }

    liveData.addSource(rightSrc) { rightVal ->
        rightVal?.let {
            leftSrc.value?.let { liveData.value = onChanged(it, rightVal) }
        }
    }
    return liveData
}

fun <X, Y, Z> combineLatestWithNull(
    leftSrc: LiveData<X?>,
    rightSrc: LiveData<Y?>,
    onChanged: (X?, Y?) -> Z
): MediatorLiveData<Z> {
    val liveData = MediatorLiveData<Z>()
    liveData.addSource(leftSrc) { liveData.value = onChanged(it, rightSrc.value) }
    liveData.addSource(rightSrc) { liveData.value = onChanged(leftSrc.value, it) }
    return liveData
}

fun <T, X, Y> MediatorLiveData<T>.addSources(
    leftSrc: LiveData<X?>,
    rightSrc: LiveData<Y?>,
    onChanged: (X?, Y?) -> T
) {
    addSource(leftSrc) { value = onChanged(it, rightSrc.value) }
    addSource(rightSrc) { value = onChanged(leftSrc.value, it) }
}

fun <T> MediatorLiveData<T>.reObserveFirst(src: LiveData<T?>, onChanged: (T?) -> Unit) {
    this.removeSource(src)
    this.addSource(src) {
        this.removeSource(src)
        onChanged(it)
    }
}

fun <T> LiveData<T>.filter(predicate: (T?) -> Boolean): MediatorLiveData<T> {
    return mediatorLiveData(this) {
        if (predicate(it)) {
            this.postValue(it)
        }
    }
}
