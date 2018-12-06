package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseResource
import io.hoopit.firebasecomponents.core.Scope
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

class FirebaseListQueryCache<K : Comparable<K>, T : FirebaseResource>(
    private val scope: Scope,
    query: Query,
    clazz: KClass<T>,
    orderKeyFunction: (T) -> K
) : FirebaseManagedQueryCache<K, T>(scope, query, clazz, orderKeyFunction) {

    private var liveData: MutableLiveData<List<T>>? = null

    fun getLiveData(query: Query, disconnectDelay: Long, resource: Scope.Resource = scope.getResource(query)): LiveData<List<T>> {
        if (liveData == null) {
            liveData = FirebaseCacheLiveData<List<T>>(scope.getResource(query), query, this, disconnectDelay).also {
                if (resource.rootQuery == query) resource.addListener(getListener())
            }
        }
        return requireNotNull(liveData)
    }

    override fun invalidate() {
        liveData?.postValue(collection.toList())
    }
}
