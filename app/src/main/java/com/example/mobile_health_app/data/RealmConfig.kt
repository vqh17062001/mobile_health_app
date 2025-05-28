package com.example.mobile_health_app.data

import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.AppConfiguration

object RealmConfig {
    private const val APP_ID = "applicationmobile-ngrhvxm" // Thay bằng App ID của bạn

    val app: App by lazy {
        App.create(
            AppConfiguration.Builder(APP_ID)
                .build()
        )
    }
}
