package com.artemidius.bloodpressure.auth

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class AuthRepoImpl @Inject constructor(private val auth: FirebaseAuth) : AuthRepository {
    override fun isUserAuthorised(): Boolean {
        val currentUser = auth.currentUser
        return currentUser != null
    }
    override fun getUserId(): String? {
        val currentUser = auth.currentUser
        return currentUser?.uid
    }
}
