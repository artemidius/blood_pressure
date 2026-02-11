package com.artemidius.bloodpressure.auth

import javax.inject.Inject

class AuthRepoMock @Inject constructor() : AuthRepository {
    override fun isUserAuthorised(): Boolean = false
    override fun getUserId(): String = "appTest_001"
}