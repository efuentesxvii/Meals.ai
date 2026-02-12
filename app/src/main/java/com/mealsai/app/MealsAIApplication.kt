package com.mealsai.app

import android.app.Application
import com.google.firebase.FirebaseApp

class MealsAIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
