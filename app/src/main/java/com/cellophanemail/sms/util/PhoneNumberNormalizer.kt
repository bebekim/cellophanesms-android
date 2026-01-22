package com.cellophanemail.sms.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Normalizes phone numbers for consistent sender ID aggregation.
 *
 * Supports:
 * - Australian mobile numbers (04xx, +614xx)
 * - Australian landline numbers (02, 03, 07, 08, +612, etc.)
 * - International E.164 format
 * - Short codes and alphanumeric sender IDs
 */
@Singleton
class PhoneNumberNormalizer @Inject constructor() {

    /**
     * Normalizes a phone number/sender ID for consistent storage and aggregation.
     *
     * @param address Raw sender address (phone number or alphanumeric)
     * @param defaultCountryCode Default country code if not specified (default: AU = 61)
     * @return Normalized sender ID
     */
    fun normalize(address: String, defaultCountryCode: String = "61"): String {
        val cleaned = address.trim()

        // Handle alphanumeric sender IDs (e.g., "MyBank", "UBER")
        if (isAlphanumericSenderId(cleaned)) {
            return cleaned.uppercase()
        }

        // Handle short codes (typically 4-6 digits)
        if (isShortCode(cleaned)) {
            return cleaned
        }

        // Remove all non-digit characters except leading +
        val hasPlus = cleaned.startsWith("+")
        val digitsOnly = cleaned.replace(Regex("[^0-9]"), "")

        if (digitsOnly.isEmpty()) {
            return cleaned.uppercase() // Fallback for weird formats
        }

        return when {
            // Already E.164 format with +
            hasPlus && digitsOnly.length >= 10 -> normalizeE164(digitsOnly)

            // Australian mobile starting with 04
            digitsOnly.startsWith("04") && digitsOnly.length == 10 -> {
                // Convert 04xx to 614xx format (without +)
                "61${digitsOnly.substring(1)}"
            }

            // Australian number starting with 0 (landline or mobile)
            digitsOnly.startsWith("0") && digitsOnly.length == 10 -> {
                "61${digitsOnly.substring(1)}"
            }

            // International number starting with country code (no leading 0)
            digitsOnly.length >= 10 && !digitsOnly.startsWith("0") -> {
                normalizeE164(digitsOnly)
            }

            // Short Australian number without leading 0 - assume AU
            digitsOnly.length == 9 && (digitsOnly.startsWith("4") ||
                digitsOnly.startsWith("2") || digitsOnly.startsWith("3") ||
                digitsOnly.startsWith("7") || digitsOnly.startsWith("8")) -> {
                "61$digitsOnly"
            }

            // Fallback: return digits only
            else -> digitsOnly
        }
    }

    /**
     * Extracts the last N significant digits for display/comparison.
     * Useful for matching numbers that might have different country code formats.
     */
    fun getSignificantDigits(normalizedNumber: String, count: Int = 9): String {
        val digits = normalizedNumber.replace(Regex("[^0-9]"), "")
        return if (digits.length >= count) {
            digits.takeLast(count)
        } else {
            digits
        }
    }

    /**
     * Checks if two normalized numbers likely represent the same sender.
     */
    fun isSameSender(normalized1: String, normalized2: String): Boolean {
        if (normalized1 == normalized2) return true

        // Compare last 9 significant digits (covers AU mobile numbers)
        val sig1 = getSignificantDigits(normalized1)
        val sig2 = getSignificantDigits(normalized2)

        return sig1 == sig2 && sig1.length >= 8
    }

    private fun isAlphanumericSenderId(address: String): Boolean {
        // Contains letters and is reasonably short (typical sender ID)
        return address.any { it.isLetter() } && address.length <= 11
    }

    private fun isShortCode(address: String): Boolean {
        val digits = address.replace(Regex("[^0-9]"), "")
        // Short codes are typically 4-6 digits with no letters
        return digits.length in 4..6 && !address.any { it.isLetter() }
    }

    private fun normalizeE164(digits: String): String {
        // Common country codes to preserve
        return when {
            // Australia
            digits.startsWith("61") -> digits
            // US/Canada
            digits.startsWith("1") && digits.length == 11 -> digits
            // UK
            digits.startsWith("44") -> digits
            // Others - keep as-is
            else -> digits
        }
    }

    companion object {
        /**
         * Static utility for simple normalization without DI.
         * Prefer injected instance for consistency.
         */
        fun normalizeStatic(address: String): String {
            return PhoneNumberNormalizer().normalize(address)
        }
    }
}
