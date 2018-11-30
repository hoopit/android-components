package io.hoopit.firebasecomponents.lifecycle

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseChildEventListener
import io.hoopit.firebasecomponents.core.FirebaseCollection
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import kotlin.reflect.KClass

class FirebaseListLiveData<K : Comparable<K>, T : IFirebaseEntity>(
    private val query: Query,
    private val classModel: KClass<out T>,
    private val collection: FirebaseCollection<K, T>
) : BaseFirebaseLiveData<List<T>>() {

    constructor(
        query: Query,
        classModel: KClass<out T>,
        orderKeyFunction: (T) -> K
    ) : this(query, classModel, FirebaseCollection<K, T>(orderKeyFunction, query.spec.params.isViewFromLeft))


    private val listener = object : FirebaseChildEventListener<T>(classModel = classModel) {

        override fun cancelled(error: DatabaseError) {
            TODO("not implemented")
        }

        override fun childMoved(previousChildName: String?, child: T) {
            TODO("not implemented")
        }

        override fun childChanged(previousChildName: String?, child: T) {
            collection.update(previousChildName, child)
        }

        override fun childAdded(previousChildName: String?, child: T) {
            collection.addAfter(previousChildName, child)
            postValue(collection.toList())
        }

        override fun childRemoved(child: T) {
            collection.remove(child)
            postValue(collection.toList())
        }
    }

    override fun addListener() {
        query.addChildEventListener(listener)
    }

    override fun removeListener() {
        query.removeEventListener(listener)
    }
}
