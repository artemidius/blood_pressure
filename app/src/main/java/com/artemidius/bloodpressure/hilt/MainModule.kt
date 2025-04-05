package com.artemidius.bloodpressure.hilt

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetManager
import androidx.health.connect.client.HealthConnectClient
import com.artemidius.bloodpressure.R
import com.artemidius.bloodpressure.auth.AuthRepoImpl
import com.artemidius.bloodpressure.auth.AuthRepository
import com.artemidius.bloodpressure.data.FileRepository
import com.artemidius.bloodpressure.data.FileRepositoryImpl
import com.artemidius.bloodpressure.data.StorageRepository
import com.artemidius.bloodpressure.data.StorageRepositoryImpl
import com.artemidius.bloodpressure.health.connect.HealthConnectRepository
import com.artemidius.bloodpressure.health.connect.HealthConnectRepositoryImpl
import com.artemidius.bloodpressure.ml.TextRecognizer
import com.artemidius.bloodpressure.ml.TextRecognizerImpl
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MainModule {
    @Binds
    abstract fun bindAuthRepo(impl: AuthRepoImpl): AuthRepository

    @Binds
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    abstract fun bindTextRecognizer(impl: TextRecognizerImpl): TextRecognizer

    @Binds
    abstract fun bindHealthConnectRepo(impl: HealthConnectRepositoryImpl): HealthConnectRepository

    companion object {
        @Provides
        fun provideAssetManager(@ApplicationContext appContext: Context): AssetManager =
            appContext.assets

        @Provides
        fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

        @Provides
        fun provideSharedPreferences(@ApplicationContext appContext: Context): SharedPreferences =
            appContext.getSharedPreferences(appContext.getString(R.string.shared_prefs), Context.MODE_PRIVATE)

        @Provides
        fun provideHealthConnectClient(@ApplicationContext appContext: Context): HealthConnectClient? =
            try {
                HealthConnectClient.getOrCreate(appContext)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
    }
}