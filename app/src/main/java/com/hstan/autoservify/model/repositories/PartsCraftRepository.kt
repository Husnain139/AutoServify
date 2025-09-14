package com.hstan.autoservify.model.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PartsCraftRepository {
    val PartsCraftCollection = FirebaseFirestore.getInstance().collection("Partscrafts")


    suspend fun savePartsCraft(PartsCraft: PartsCraft): Result<Boolean> {
        try {
            val document = PartsCraftCollection.document()
            PartsCraft.id = document.id
            document.set(PartsCraft).await()
            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun getPartsCrafts() =
        PartsCraftCollection.snapshots().map { it.toObjects(PartsCraft::class.java) }

    suspend fun deletePartsCraft(partId: String): Result<Boolean> {
        return try {
            PartsCraftCollection.document(partId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePartsCraft(partsCraft: PartsCraft): Result<Boolean> {
        return try {
            partsCraft.id?.let { id ->
                PartsCraftCollection.document(id).set(partsCraft).await()
                Result.success(true)
            } ?: Result.failure(Exception("Part ID is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
