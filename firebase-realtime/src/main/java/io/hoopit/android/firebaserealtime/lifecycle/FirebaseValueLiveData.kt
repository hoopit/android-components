package io.hoopit.android.firebaserealtime.lifecycle

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.common.livedata.DelayedDisconnectLiveData
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import io.hoopit.android.firebaserealtime.ext.getValueOrNull
import kotlin.reflect.KClass

class FirebaseValueLiveData<T : Any>(
    private val query: Query?,
    private val classModel: KClass<T>,
    disconnectDelay: Long
) : DelayedDisconnectLiveData<T?>(disconnectDelay), ValueEventListener {
    override fun delayedOnActive() {
        query?.addValueEventListener(this)
    }

    override fun delayedOnInactive() {
        query?.removeEventListener(this)
    }

    override fun onCancelled(error: DatabaseError) {}

    override fun onDataChange(snapshot: DataSnapshot) {
        val item = snapshot.getValueOrNull(classModel)
        if (item is IFirebaseEntity) item.init(snapshot)
        postValue(item)
    }
}


