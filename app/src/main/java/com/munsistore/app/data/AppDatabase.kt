package com.munsistore.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY isFeatured DESC, name ASC")
    fun getAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): Product?

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE sellerId = :sellerId ORDER BY createdAt DESC")
    fun getBySeller(sellerId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isFlashSale = 1 ORDER BY name ASC")
    fun getFlashSale(): Flow<List<Product>>

    @Query("SELECT DISTINCT brand FROM products ORDER BY brand ASC")
    fun getBrands(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR nameBn LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE products SET soldCount = soldCount + :qty WHERE id = :productId")
    suspend fun addSold(productId: String, qty: Int)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getAll(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartItem)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun delete(productId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items")
    fun getAll(): Flow<List<WishlistItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: WishlistItem)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun delete(productId: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE userEmail = :email ORDER BY createdAt DESC")
    fun getByUser(email: String): Flow<List<Order>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): Order?

    @Query("SELECT COUNT(*) FROM orders WHERE userEmail = :email AND status = 'DELIVERED'")
    suspend fun deliveredCountForUser(email: String): Int

    @Query("SELECT COALESCE(SUM(totalAmount), 0) FROM orders WHERE status != 'CANCELLED'")
    fun totalSales(): Flow<Double>

    @Query("SELECT COUNT(*) FROM orders WHERE status = 'PLACED' OR status = 'CONFIRMED'")
    fun pendingCount(): Flow<Int>

    /** Orders that contain at least one product of this seller (multi-vendor). */
    @Query(
        "SELECT DISTINCT orders.* FROM orders " +
            "JOIN order_items ON order_items.orderId = orders.id " +
            "JOIN products ON products.id = order_items.productId " +
            "WHERE products.sellerId = :sellerEmail ORDER BY orders.createdAt DESC"
    )
    fun getOrdersForSeller(sellerEmail: String): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: Order)

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateStatus(orderId: String, status: String)

    @Query("UPDATE orders SET status = :status, courierName = :courier WHERE id = :orderId")
    suspend fun updateStatusWithCourier(orderId: String, status: String, courier: String)
}

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getByOrderId(orderId: String): List<OrderItem>

    @Insert
    suspend fun insertAll(items: List<OrderItem>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE referralCode = :code")
    suspend fun getByReferralCode(code: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Query("UPDATE users SET coins = coins + :delta WHERE email = :email")
    suspend fun addCoins(email: String, delta: Int)

    @Query("UPDATE users SET isSeller = 1, shopName = :shopName WHERE email = :email")
    suspend fun registerSeller(email: String, shopName: String)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE productId = :productId ORDER BY createdAt DESC")
    fun getByProduct(productId: String): Flow<List<Review>>

    @Query("SELECT AVG(rating) FROM reviews WHERE productId = :productId")
    suspend fun averageFor(productId: String): Double?

    @Query("SELECT COUNT(*) FROM reviews WHERE productId = :productId")
    suspend fun countFor(productId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(review: Review)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE productId = :productId ORDER BY createdAt DESC")
    fun getByProduct(productId: String): Flow<List<ProductQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: ProductQuestion)

    @Query("UPDATE questions SET answer = :answer WHERE id = :id")
    suspend fun answer(id: String, answer: String)
}

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers ORDER BY code ASC")
    fun getAll(): Flow<List<Voucher>>

    @Query("SELECT * FROM vouchers WHERE code = :code")
    suspend fun getByCode(code: String): Voucher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(voucher: Voucher)

    @Query("UPDATE vouchers SET active = :active WHERE code = :code")
    suspend fun setActive(code: String, active: Boolean)

    @Delete
    suspend fun delete(voucher: Voucher)
}

@Dao
interface AddressDao {
    @Query("SELECT * FROM addresses WHERE userEmail = :email ORDER BY isDefault DESC, label ASC")
    fun getByUser(email: String): Flow<List<Address>>

    @Query("SELECT * FROM addresses WHERE id = :id")
    suspend fun getById(id: String): Address?

    @Query("SELECT * FROM addresses WHERE userEmail = :email AND isDefault = 1 LIMIT 1")
    suspend fun getDefault(email: String): Address?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: Address)

    @Query("UPDATE addresses SET isDefault = 0 WHERE userEmail = :email")
    suspend fun clearDefaults(email: String)

    @Delete
    suspend fun delete(address: Address)
}

@Dao
interface RecentViewDao {
    @Query("SELECT * FROM recent_views ORDER BY viewedAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<RecentView>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(view: RecentView)

    @Query("DELETE FROM recent_views")
    suspend fun clear()
}

@Dao
interface SearchTermDao {
    @Query("SELECT * FROM search_terms ORDER BY count DESC, lastSearchedAt DESC LIMIT :limit")
    fun getTrending(limit: Int): Flow<List<SearchTerm>>

    @Query("SELECT * FROM search_terms WHERE term = :term")
    suspend fun get(term: String): SearchTerm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(term: SearchTerm)
}

@Dao
interface ReturnDao {
    @Query("SELECT * FROM return_requests WHERE userEmail = :email ORDER BY createdAt DESC")
    fun getByUser(email: String): Flow<List<ReturnRequest>>

    @Query("SELECT * FROM return_requests ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ReturnRequest>>

    @Query("SELECT * FROM return_requests WHERE orderId = :orderId")
    suspend fun getByOrder(orderId: String): ReturnRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: ReturnRequest)

    @Query("UPDATE return_requests SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface FlashSaleDao {
    @Query("SELECT * FROM flash_sale WHERE id = 1")
    suspend fun get(): FlashSale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: FlashSale)
}

@Database(
    entities = [
        Product::class, CartItem::class, WishlistItem::class, Order::class,
        OrderItem::class, User::class, Review::class, ProductQuestion::class,
        Voucher::class, Address::class, RecentView::class, SearchTerm::class,
        ReturnRequest::class, FlashSale::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun userDao(): UserDao
    abstract fun reviewDao(): ReviewDao
    abstract fun questionDao(): QuestionDao
    abstract fun voucherDao(): VoucherDao
    abstract fun addressDao(): AddressDao
    abstract fun recentViewDao(): RecentViewDao
    abstract fun searchTermDao(): SearchTermDao
    abstract fun returnDao(): ReturnDao
    abstract fun flashSaleDao(): FlashSaleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "munsi_store.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
