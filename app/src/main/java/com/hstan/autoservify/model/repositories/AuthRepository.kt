package com.hstan.autoservify.model.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.hstan.autoservify.model.AppUser
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun logout(): Result<Boolean> {
        FirebaseAuth.getInstance().signOut()
        return Result.success(true)
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        try {
            val result = FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            return Result.success(result.user!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun signup(email: String, password: String, name: String): Result<FirebaseUser> {
        try {
            val result = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            result.user?.updateProfile(profileUpdates)?.await()
            return Result.success(result.user!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Boolean> {
        try {
            val result = FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun getCurrentUserEmail(): String? {
        return FirebaseAuth.getInstance().currentUser?.email
    }

    suspend fun saveUserProfile(user: AppUser): Result<Boolean> {
        return try {
            db.collection("users").document(user.uid).set(user).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<AppUser> {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(AppUser::class.java) ?: AppUser()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserShopId(uid: String, shopId: String): Result<Boolean> {
        return try {
            db.collection("users").document(uid)
                .update("shopId", shopId).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}