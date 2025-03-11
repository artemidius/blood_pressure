package com.artemidius.bloodpressure.auth

import com.firebase.ui.auth.AuthUI

private val providers = arrayListOf(
    AuthUI.IdpConfig.GoogleBuilder().build(),
    AuthUI.IdpConfig.AnonymousBuilder().build(),
)

val signInIntent = AuthUI.getInstance()
    .createSignInIntentBuilder()
    .setAvailableProviders(providers)
    .setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)
    .build()