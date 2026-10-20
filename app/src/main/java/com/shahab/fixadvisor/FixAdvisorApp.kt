package com.shahab.fixadvisor

import android.app.Application
import android.content.Context
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.shahab.fixadvisor.data.repo.AuthManager
import com.shahab.fixadvisor.data.repo.ReviewRepository
import com.shahab.fixadvisor.data.repo.ShopRepository
import com.shahab.fixadvisor.data.repo.UserRepository

/** Simple manual DI container (no Hilt/Dagger — keeps dependencies minimal). */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val authManager = AuthManager(appContext)
    val shopRepository = ShopRepository(appContext)
    val reviewRepository = ReviewRepository(appContext)
    val userRepository = UserRepository(appContext)
}

class FixAdvisorApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        // Enable Firestore offline persistence BEFORE any Firestore usage.
        // Must be set before the first Firebase.firestore access.
        // Reads work through offline periods; writes queue until back online.
        runCatching {
            Firebase.firestore.firestoreSettings = firestoreSettings {
                isPersistenceEnabled = true
            }
        }

        container = AppContainer(this)
    }
}
