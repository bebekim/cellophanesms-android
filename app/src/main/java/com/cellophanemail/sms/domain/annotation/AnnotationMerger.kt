package com.cellophanemail.sms.domain.annotation

import com.cellophanemail.sms.domain.model.TextAnnotation

object AnnotationMerger {

    /**
     * Merges annotations from multiple sources, resolving overlaps.
     *
     * Priority rules (in order):
     * 1. Higher [TextAnnotation.priority] wins
     * 2. Higher [TextAnnotation.confidence] breaks ties
     * 3. Longer span breaks remaining ties
     *
     * Partial overlaps: the lower-priority annotation is truncated to
     * the non-overlapping region. If the remaining span is <2 chars,
     * the annotation is discarded entirely.
     */
    fun merge(annotations: List<TextAnnotation>): List<TextAnnotation> {
        if (annotations.size <= 1) return annotations

        val sorted = annotations.sortedWith(
            compareByDescending<TextAnnotation> { it.priority }
                .thenByDescending { it.confidence }
                .thenByDescending { it.length }
        )

        val accepted = mutableListOf<TextAnnotation>()

        for (candidate in sorted) {
            val truncated = resolveOverlaps(candidate, accepted)
            if (truncated != null && truncated.length >= 2) {
                accepted.add(truncated)
            }
        }

        return accepted.sortedBy { it.startIndex }
    }

    private fun resolveOverlaps(
        candidate: TextAnnotation,
        accepted: List<TextAnnotation>
    ): TextAnnotation? {
        var current = candidate

        for (existing in accepted) {
            if (!current.overlaps(existing)) continue

            // Full containment — discard candidate
            if (current.startIndex >= existing.startIndex && current.endIndex <= existing.endIndex) {
                return null
            }

            // Candidate extends before the existing span
            if (current.startIndex < existing.startIndex && current.endIndex <= existing.endIndex) {
                current = current.copy(endIndex = existing.startIndex)
            }
            // Candidate extends after the existing span
            else if (current.startIndex >= existing.startIndex && current.endIndex > existing.endIndex) {
                current = current.copy(startIndex = existing.endIndex)
            }
            // Candidate wraps the existing span — keep the longer leading portion
            else if (current.startIndex < existing.startIndex && current.endIndex > existing.endIndex) {
                val leadingLen = existing.startIndex - current.startIndex
                val trailingLen = current.endIndex - existing.endIndex
                current = if (leadingLen >= trailingLen) {
                    current.copy(endIndex = existing.startIndex)
                } else {
                    current.copy(startIndex = existing.endIndex)
                }
            }

            if (current.length < 2) return null
        }

        return current
    }
}
