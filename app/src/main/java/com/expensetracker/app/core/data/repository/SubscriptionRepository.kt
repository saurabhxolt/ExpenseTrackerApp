package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.SubscriptionDao
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) {
    val allSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    suspend fun saveSubscription(subscription: SubscriptionEntity): Long {
        return subscriptionDao.insertSubscription(subscription)
    }

    suspend fun updateSubscription(subscription: SubscriptionEntity) {
        subscriptionDao.updateSubscription(subscription)
    }

    suspend fun deleteSubscription(subscription: SubscriptionEntity) {
        subscriptionDao.deleteSubscription(subscription)
    }

    suspend fun findSubscriptionByName(name: String): SubscriptionEntity? {
        return subscriptionDao.findSubscriptionByName(name)
    }

    suspend fun getAllSubscriptionsList(): List<SubscriptionEntity> {
        return subscriptionDao.getAllSubscriptionsList()
    }
}
