package com.munsistore.app

import android.app.Application
import com.munsistore.app.data.AppDatabase
import com.munsistore.app.data.FirebaseManager
import com.munsistore.app.data.NotificationHelper
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
        NotificationHelper.ensureChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            val productDao = database.productDao()
            if (productDao.count() == 0) {
                productDao.insertAll(SeedData.products)
            }
            if (database.userDao().getByEmail(SeedData.adminUser.email) == null) {
                database.userDao().insert(SeedData.adminUser)
            }
            // One-time seeds for the v2 features (only when their tables are empty).
            if (database.voucherDao().getByCode("WELCOME10") == null) {
                SeedData.vouchers.forEach { database.voucherDao().insert(it) }
            }
            if (database.flashSaleDao().get() == null) {
                database.flashSaleDao().insert(SeedData.flashSale)
            }
            if (database.reviewDao().countFor("p01") == 0) {
                SeedData.reviews.forEach { database.reviewDao().insert(it) }
            }
            if (database.questionDao().getByProduct("p01").let {
                    kotlinx.coroutines.flow.first(it).isEmpty()
                }) {
                SeedData.questions.forEach { database.questionDao().insert(it) }
            }
            SeedData.trendingTerms.forEach { term ->
                if (database.searchTermDao().get(term.term) == null) {
                    database.searchTermDao().insert(term)
                }
            }
        }
    }
}
