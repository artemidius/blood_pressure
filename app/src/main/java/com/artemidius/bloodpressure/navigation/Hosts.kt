package com.artemidius.bloodpressure.navigation

import kotlinx.serialization.Serializable

/**
 * These are navigation destinations. Use [object] to navigate without data. Or use [data class] to
 * pass data to the destination.
 */
@Serializable
object DataInput

@Serializable
object Success

@Serializable
object PressureList

@Serializable
object Login

@Serializable
object DataGraph

@Serializable
object Camera