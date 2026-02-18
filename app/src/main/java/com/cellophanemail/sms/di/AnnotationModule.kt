package com.cellophanemail.sms.di

import com.cellophanemail.sms.data.local.NerModelManager
import com.cellophanemail.sms.data.local.NerProviderPreferences
import com.cellophanemail.sms.data.remote.NerExtractionApi
import com.cellophanemail.sms.domain.annotation.AnnotationPipeline
import com.cellophanemail.sms.domain.annotation.AnnotationSource
import com.cellophanemail.sms.domain.annotation.RegexEntitySource
import com.cellophanemail.sms.domain.annotation.ner.ClaudeCloudNerProvider
import com.cellophanemail.sms.domain.annotation.ner.GeminiNanoNerProvider
import com.cellophanemail.sms.domain.annotation.ner.NerProvider
import com.cellophanemail.sms.domain.annotation.ner.Qwen3LocalNerProvider
import com.cellophanemail.sms.domain.annotation.ner.TieredNerAnnotationSource
import com.cellophanemail.sms.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnnotationModule {

    @Provides
    @Singleton
    fun provideNerExtractionApi(retrofit: Retrofit): NerExtractionApi {
        return retrofit.create(NerExtractionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNerProviders(
        geminiNano: GeminiNanoNerProvider,
        qwen3Local: Qwen3LocalNerProvider,
        claudeCloud: ClaudeCloudNerProvider
    ): List<NerProvider> = listOf(geminiNano, qwen3Local, claudeCloud)

    @Provides
    @Singleton
    fun provideAnnotationSources(
        tieredNerSource: TieredNerAnnotationSource
    ): List<AnnotationSource> {
        return listOf(RegexEntitySource(), tieredNerSource)
    }

    @Provides
    @Singleton
    fun provideAnnotationPipeline(
        sources: List<@JvmSuppressWildcards AnnotationSource>
    ): AnnotationPipeline {
        return AnnotationPipeline(sources)
    }
}
