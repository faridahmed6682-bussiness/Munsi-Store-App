package com.munsistore.app

import android.app.Application
import com.munsistore.app.data.AppDatabase
import com.munsistore.app.data.FirebaseManager
import com.munsistore.app.data.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MunsiStoreApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        FirebaseManager.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            val productDao = database.productDao()
            if (productDao.count() == 0) {
                productDao.insertAll(SeedData.products)
            }
            if (database.userDao().getByEmail(SeedData.adminUser.email) == null) {
                database.userDao().insert(SeedData.adminUser)
            }
        }
    }
}
