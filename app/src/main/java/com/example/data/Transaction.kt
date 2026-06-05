package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["ticker"]),
        Index(value = ["date"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticker: String,
    val name: String,
    val quantity: Double,
    val purchasePrice: Double,
    val date: Long = System.currentTimeMillis(),
    val type: String, // "ACAO" or "FII"
    val isSell: Boolean = false,
    val broker: String = "",
    val sector: String = "",
    val notes: String = ""
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
