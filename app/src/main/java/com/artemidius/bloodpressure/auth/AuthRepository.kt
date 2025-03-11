package com.artemidius.bloodpressure.auth

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

interface AuthRepository {
    fun isUserAuthorised(): Boolean
    fun getUserId(): String?
}

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

class AuthRepoMock @Inject constructor() : AuthRepository {
    override fun isUserAuthorised(): Boolean = false
    override fun getUserId(): String = "appTest_001"
}