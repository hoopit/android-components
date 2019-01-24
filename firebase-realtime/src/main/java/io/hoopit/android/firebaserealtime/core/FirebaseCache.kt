package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.Query
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.android.firebaserealtime.cache.FirebaseListQueryCache
import io.hoopit.android.firebaserealtime.cache.FirebaseValueCache
import io.hoopit.android.firebaserealtime.paging.FirebasePagedListQueryCache
import kotlin.reflect.KClass

class FirebaseCache(
    private val scope: Scope
) {

    private val itemCaches = mutableMapOf<KClass<*>, FirebaseValueCache<*>>()
    private val pagedListCache = mutableMapOf<QuerySpec, FirebasePagedListQueryCache<*, *>>()
    private val listCache = mutableMapOf<QuerySpec, FirebaseListQueryCache<*, *>>()

    fun dispose(querySpec: QuerySpec) {
        pagedListCache[querySpec]?.dispose()
        pagedListCache.remove(querySpec)
    }

    fun <T : Any> getItemCache(clazz: KClass<T>): FirebaseValueCache<T> {
        @Suppress("UNCHECKED_CAST")
        return itemCaches.getOrPut(clazz) {
            FirebaseValueCache(scope, clazz)
        } as FirebaseValueCache<T>
    }

    fun <K : Comparable<K>, T : FirebaseResource> getOrCreatePagedCache(
        query: Query,
        classModel: KClass<T>,
        orderByKey: (T) -> K
    ): FirebasePagedListQueryCache<K, T> {
        // TODO: consider attaching initial listener immediately
        @Suppress("UNCHECKED_CAST")
        return pagedListCache.getOrPut(query.spec) {
            FirebasePagedListQueryCache(scope, query, classModel, orderByKey)
        } as FirebasePagedListQueryCache<K, T>
    }

    fun <K : Comparable<K>, T : FirebaseResource> getOrCreateListCache(
        query: Query,
        clazz: KClass<T>,
        orderByKey: (T) -> K
    ): FirebaseListQueryCache<K, T> {
        @Suppress("UNCHECKED_CAST")
        return listCache.getOrElse(query.spec) {
            FirebaseListQueryCache(
                scope,
                query,
                clazz,
                orderByKey
            )
        }.also {
            registerListQueryCache(
                it,
                query
            )
        } as FirebaseListQueryCache<K, T>
    }

    fun registerListQueryCache(
        cache: FirebaseListQueryCache<*, *>,
        query: Query
    ) {
        listCache.getOrPut(query.spec) { cache }
        val scope = scope.getResource(cache.query)
        return scope.addListener(cache.getChildListener())
    }
}