package com.isai.player.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Main application dependency injection module.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Dependency bindings will be populated in subsequent phases
}

