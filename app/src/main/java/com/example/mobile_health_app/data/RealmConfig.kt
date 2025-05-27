package com.example.mobile_health_app.data

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.AppConfiguration

import com.example.mobile_health_app.data.model.*

object RealmConfig {
    private const val APP_ID = "applicationmobile-ngrhvxm" // Lấy từ MongoDB Atlas

    val app: App by lazy {
        App.create(
            AppConfiguration.Builder(APP_ID)
                .build()
        )
    }

    fun getRealm(): Realm {
        val config = RealmConfiguration.Builder(
            schema = setOf(User::class)
        ).build()
        return Realm.open(config)
    }
}

