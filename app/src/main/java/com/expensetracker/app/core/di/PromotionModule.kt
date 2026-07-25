package com.expensetracker.app.core.di

import android.content.Context
import com.expensetracker.app.core.promotions.PromotionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PromotionModule {

    @Provides
    @Singleton
    fun providePromotionManager(
        @ApplicationContext context: Context
    ): PromotionManager {
        return PromotionManager(context)
    }
}
