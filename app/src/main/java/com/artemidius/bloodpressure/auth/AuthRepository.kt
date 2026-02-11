package com.artemidius.bloodpressure.auth

interface AuthRepository {
    fun isUserAuthorised(): Boolean
    fun getUserId(): String?
}

