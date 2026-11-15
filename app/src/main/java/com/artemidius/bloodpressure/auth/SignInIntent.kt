package com.artemidius.bloodpressure.auth

import com.firebase.ui.auth.AuthUI
import com.artemidius.bloodpressure.R

fun signInIntent(upgrade: Boolean = false) = AuthUI.getInstance().createSignInIntentBuilder()
    .setAvailableProviders(if (upgrade) listOf(AuthUI.IdpConfig.GoogleBuilder().build()) else listOf(
        AuthUI.IdpConfig.GoogleBuilder().build(), AuthUI.IdpConfig.AnonymousBuilder().build()))
    .setTheme(R.style.Theme_BloodPressure)
    .apply { if (upgrade) enableAnonymousUsersAutoUpgrade() }
    .build()
