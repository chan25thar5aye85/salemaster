package com.akari.retailer.core.utils

/**
 * Maps raw Firestore / gRPC exception messages to short, user-readable
 * strings. Called from every save path so the user sees something
 * meaningful instead of "UNAVAILABLE: Channel shutdownNow invoked".
 *
 * The distinction matters:
 *   - "unavailable" / "channel shutdown" / "network"
 *       → write did NOT land. User needs to retry.
 *   - "deadline" / "timeout"
 *       → server didn't respond in time. Write did NOT land.
 *   - "permission" / "unauthenticated"
 *       → configuration issue. Write did NOT land.
 *   - anything else
 *       → pass the raw message through.
 */
object FirestoreErrorFormatter {

    fun friendlyMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Save failed. Please try again."

        val lower = raw.lowercase()

        return when {
            "unavailable" in lower ||
            "channel shutdown" in lower ||
            "channel_shutdown" in lower ||
            "connection" in lower -> "Connection lost. Not saved — please try again."

            "deadline" in lower ||
            "timeout" in lower ||
            "timed out" in lower -> "Connection is too slow. Not saved — please try again."

            "permission" in lower ||
            "unauthenticated" in lower -> "Not authorised to save. Please contact support."

            "network" in lower -> "Network unavailable. Not saved — please try again."

            "not found" in lower -> "A required record was not found. Not saved."

            else -> raw
        }
    }
}
