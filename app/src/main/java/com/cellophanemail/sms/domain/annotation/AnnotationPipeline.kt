package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.TextAnnotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationPipeline @Inject constructor(
    private val sources: List<@JvmSuppressWildcards AnnotationSource>
) {
    private val cacheMutex = Mutex()
    private val cache = LinkedHashMap<String, List<TextAnnotation>>(CACHE_SIZE, 0.75f, true)

    suspend fun annotate(
        text: String,
        enabledSourceIds: Set<String>? = null
    ): List<TextAnnotation> {
        if (text.isBlank()) return emptyList()

        val cacheKey = buildCacheKey(text, enabledSourceIds)
        cacheMutex.withLock { cache[cacheKey] }?.let { return it }

        val activeSources = if (enabledSourceIds != null) {
            sources.filter { it.sourceId in enabledSourceIds }
        } else {
            sources
        }

        val allAnnotations = activeSources.flatMap { source ->
            source.annotate(text)
        }

        val merged = AnnotationMerger.merge(allAnnotations)
        cacheMutex.withLock {
            cache[cacheKey] = merged
            while (cache.size > CACHE_SIZE) {
                cache.remove(cache.keys.first())
            }
        }
        return merged
    }

    /**
     * Progressive annotation: emits regex results instantly, then re-emits
     * with NER results merged in once the slower sources complete.
     *
     * Emits at least once (instant sources). Emits a second time only if
     * deferred sources produce additional annotations.
     */
    fun annotateProgressive(
        text: String,
        enabledSourceIds: Set<String>? = null
    ): Flow<List<TextAnnotation>> = flow {
        if (text.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val activeSources = if (enabledSourceIds != null) {
            sources.filter { it.sourceId in enabledSourceIds }
        } else {
            sources
        }

        val (instantSources, deferredSources) = activeSources.partition { !it.requiresNetwork && it.sourceId == RegexEntitySource.SOURCE_ID }

        // Phase 1: Instant (regex) annotations
        val instantAnnotations = instantSources.flatMap { it.annotate(text) }
        val instantMerged = AnnotationMerger.merge(instantAnnotations)
        emit(instantMerged)

        // Phase 2: Deferred (NER) annotations
        if (deferredSources.isNotEmpty()) {
            val deferredAnnotations = deferredSources.flatMap { source ->
                runCatching { source.annotate(text) }.getOrDefault(emptyList())
            }

            if (deferredAnnotations.isNotEmpty()) {
                val allMerged = AnnotationMerger.merge(instantAnnotations + deferredAnnotations)
                emit(allMerged)

                // Cache the final merged result
                val cacheKey = buildCacheKey(text, enabledSourceIds)
                cacheMutex.withLock {
                    cache[cacheKey] = allMerged
                    while (cache.size > CACHE_SIZE) {
                        cache.remove(cache.keys.first())
                    }
                }
            }
        }
    }

    suspend fun invalidate(text: String) {
        val prefix = "$text:"
        cacheMutex.withLock {
            cache.keys.removeAll { it.startsWith(prefix) }
        }
    }

    suspend fun clearCache() {
        cacheMutex.withLock { cache.clear() }
    }

    private fun buildCacheKey(text: String, enabledSourceIds: Set<String>?): String {
        val sourceKey = enabledSourceIds?.sorted()?.joinToString(",") ?: "all"
        return "$text:$sourceKey"
    }

    companion object {
        private const val CACHE_SIZE = 128
    }
}
