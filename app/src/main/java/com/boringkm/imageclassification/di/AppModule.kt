package com.boringkm.imageclassification.di

import android.content.Context
import com.boringkm.imageclassification.tflite.ClassifierWithModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.tensorflow.lite.support.model.Model
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideTFLiteModel(model: Model): Model = model

    @Singleton
    @Provides
    fun provideModelProvider(@ApplicationContext context: Context): ClassifierWithModel =
        ClassifierWithModel(context, ClassifierWithModel.IMAGENET_CLASSIFY_MODEL)
}