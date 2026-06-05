package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val notificationDao: NotificationDao,
    private val changelogDao: ChangelogDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun getAllTransactionsSync(): List<Transaction> {
        return transactionDao.getAllTransactionsSync()
    }

    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun insertAll(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Int) {
        transactionDao.deleteById(id)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    // --- NOTIFICATIONS API ---
    val allNotifications: Flow<List<DbNotification>> = notificationDao.getAllNotifications()

    suspend fun getAllNotificationsSync(): List<DbNotification> {
        return notificationDao.getAllNotificationsSync()
    }

    suspend fun insertNotification(notification: DbNotification) {
        notificationDao.insertNotification(notification)
    }

    suspend fun insertAllNotifications(notifications: List<DbNotification>) {
        notificationDao.insertAll(notifications)
    }

    suspend fun markAllNotificationsAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteNotificationById(id: Int) {
        notificationDao.deleteById(id)
    }

    suspend fun deleteAllNotifications() {
        notificationDao.deleteAll()
    }

    // --- CHANGELOGS API ---
    val allChangelogs: Flow<List<DbChangelog>> = changelogDao.getAllChangelogs()

    suspend fun insertChangelog(changelog: DbChangelog) {
        changelogDao.insertChangelog(changelog)
    }

    suspend fun insertAllChangelogs(changelogs: List<DbChangelog>) {
        changelogDao.insertAll(changelogs)
    }
}
