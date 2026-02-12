package com.cellophanemail.sms.debug

import com.cellophanemail.sms.data.local.dao.MessageDao
import com.cellophanemail.sms.data.local.dao.SenderSummaryDao
import com.cellophanemail.sms.data.local.dao.ThreadDao
import com.cellophanemail.sms.data.local.entity.MessageEntity
import com.cellophanemail.sms.data.local.entity.SenderSummaryEntity
import com.cellophanemail.sms.data.local.entity.ThreadEntity
import com.cellophanemail.sms.util.PhoneNumberNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Seeds the database with realistic test data for UI development and testing.
 * Generates senders with various risk profiles and messages with different classifications.
 */
@Singleton
class TestDataSeeder @Inject constructor(
    private val messageDao: MessageDao,
    private val threadDao: ThreadDao,
    private val senderSummaryDao: SenderSummaryDao,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
) {
    suspend fun seedTestData() = withContext(Dispatchers.IO) {
        // Clear existing data first
        clearAllData()

        // Seed senders and their messages
        seedHighRiskSender()
        seedMediumRiskSender()
        seedLowRiskSender()
        seedSafeSenders()
        seedBusinessSenders()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        messageDao.deleteAll()
        threadDao.deleteAll()
        senderSummaryDao.deleteAll()
    }

    private suspend fun seedHighRiskSender() {
        val sender = TestSender(
            name = "Alex (Ex)",
            phone = "0412345678",
            photoUri = null
        )

        val messages = listOf(
            // TOXIC_LOGISTICS - criticism + logistics
            TestMessage(
                body = "You NEVER pick up the kids on time. They're waiting at school AGAIN. Be there by 4pm.",
                category = "TOXIC_LOGISTICS",
                severity = "HIGH",
                hasLogistics = true,
                horsemen = listOf("CRITICISM" to 0.85f),
                daysAgo = 0
            ),
            // TOXIC_LOGISTICS - contempt + logistics
            TestMessage(
                body = "I can't believe I ever trusted you with anything. The court date is March 15th. Try not to mess this up too.",
                category = "TOXIC_LOGISTICS",
                severity = "HIGH",
                hasLogistics = true,
                horsemen = listOf("CONTEMPT" to 0.78f, "CRITICISM" to 0.45f),
                daysAgo = 1
            ),
            // TOXIC_NOISE - pure contempt
            TestMessage(
                body = "You're pathetic. Everyone knows it. Your own family doesn't even respect you.",
                category = "TOXIC_NOISE",
                severity = "CRITICAL",
                hasLogistics = false,
                horsemen = listOf("CONTEMPT" to 0.92f),
                daysAgo = 2
            ),
            // TOXIC_LOGISTICS - defensiveness + logistics
            TestMessage(
                body = "It's not MY fault the payment was late. YOU changed the account. Send the $500 to the new account by Friday.",
                category = "TOXIC_LOGISTICS",
                severity = "MEDIUM",
                hasLogistics = true,
                horsemen = listOf("DEFENSIVENESS" to 0.71f),
                daysAgo = 3
            ),
            // TOXIC_NOISE - stonewalling
            TestMessage(
                body = "...",
                category = "TOXIC_NOISE",
                severity = "LOW",
                hasLogistics = false,
                horsemen = listOf("STONEWALLING" to 0.65f),
                daysAgo = 4
            ),
            // TOXIC_LOGISTICS
            TestMessage(
                body = "Whatever. Just sign the papers. Drop them at 123 Main St by Tuesday or I'm calling my lawyer.",
                category = "TOXIC_LOGISTICS",
                severity = "MEDIUM",
                hasLogistics = true,
                horsemen = listOf("STONEWALLING" to 0.55f, "CONTEMPT" to 0.40f),
                daysAgo = 5
            ),
            // More toxic messages
            TestMessage(
                body = "You always do this. You ALWAYS put yourself first. The kids need their school supplies - $200.",
                category = "TOXIC_LOGISTICS",
                severity = "HIGH",
                hasLogistics = true,
                horsemen = listOf("CRITICISM" to 0.88f),
                daysAgo = 7
            ),
            TestMessage(
                body = "I'm disgusted that you think this is acceptable parenting.",
                category = "TOXIC_NOISE",
                severity = "HIGH",
                hasLogistics = false,
                horsemen = listOf("CONTEMPT" to 0.82f),
                daysAgo = 10
            ),
            // A safe message mixed in
            TestMessage(
                body = "Fine. Thursday works for pickup.",
                category = "SAFE_LOGISTICS",
                severity = null,
                hasLogistics = true,
                horsemen = emptyList(),
                daysAgo = 6
            )
        )

        insertSenderWithMessages(sender, messages)
    }

    private suspend fun seedMediumRiskSender() {
        val sender = TestSender(
            name = "Jordan",
            phone = "0423456789",
            photoUri = null
        )

        val messages = listOf(
            TestMessage(
                body = "You said you'd help move but you never showed. Typical.",
                category = "TOXIC_NOISE",
                severity = "MEDIUM",
                hasLogistics = false,
                horsemen = listOf("CRITICISM" to 0.65f),
                daysAgo = 1
            ),
            TestMessage(
                body = "Can you at least return my books? I need them for work Monday.",
                category = "TOXIC_LOGISTICS",
                severity = "LOW",
                hasLogistics = true,
                horsemen = listOf("CRITICISM" to 0.35f),
                daysAgo = 2
            ),
            TestMessage(
                body = "Hey, are we still on for coffee Saturday?",
                category = "SAFE_LOGISTICS",
                severity = null,
                hasLogistics = true,
                horsemen = emptyList(),
                daysAgo = 3
            ),
            TestMessage(
                body = "Thanks for finally responding. 2pm works.",
                category = "SAFE_LOGISTICS",
                severity = null,
                hasLogistics = true,
                horsemen = emptyList(),
                daysAgo = 4
            ),
            TestMessage(
                body = "I don't know why I bother trying with you.",
                category = "TOXIC_NOISE",
                severity = "MEDIUM",
                hasLogistics = false,
                horsemen = listOf("CONTEMPT" to 0.58f),
                daysAgo = 8
            )
        )

        insertSenderWithMessages(sender, messages)
    }

    private suspend fun seedLowRiskSender() {
        val sender = TestSender(
            name = "Sam (Work)",
            phone = "0434567890",
            photoUri = null
        )

        val messages = listOf(
            TestMessage(
                body = "The report was supposed to be done yesterday. What happened?",
                category = "TOXIC_LOGISTICS",
                severity = "LOW",
                hasLogistics = true,
                horsemen = listOf("CRITICISM" to 0.42f),
                daysAgo = 0
            ),
            TestMessage(
                body = "Meeting moved to 3pm in Conference Room B.",
                category = "SAFE_LOGISTICS",
                severity = null,
                hasLogistics = true,
                horsemen = emptyList(),
                daysAgo = 1
            ),
            TestMessage(
                body = "Can you review the proposal before EOD?",
                category = "SAFE_LOGISTICS",
                severity = null,
                hasLogistics = true,
                horsemen = emptyList(),
                daysAgo = 2
            ),
            TestMessage(
                body = "Good work on the presentation!",
                category = "SAFE_NOISE",
                severity = null,
                hasLogistics = false,
                horsemen = emptyList(),
                daysAgo = 3
            ),
            TestMessage(
                body = "Thanks",
                category = "SAFE_NOISE",
                severity = null,
                hasLogistics = false,
                horsemen = emptyList(),
                daysAgo = 3
            )
        )

        insertSenderWithMessages(sender, messages)
    }

    private suspend fun seedSafeSenders() {
        // Mum - all safe messages
        val mum = TestSender(name = "Mum", phone = "0445678901", photoUri = null)
        val mumMessages = listOf(
            TestMessage("Dinner Sunday at 6? Dad's making roast.", "SAFE_LOGISTICS", null, true, emptyList(), 0),
            TestMessage("Don't forget your aunt's birthday next week!", "SAFE_LOGISTICS", null, true, emptyList(), 2),
            TestMessage("Love you sweetheart. Call when you can.", "SAFE_NOISE", null, false, emptyList(), 3),
            TestMessage("Picked up that book you wanted from the library.", "SAFE_LOGISTICS", null, true, emptyList(), 5),
            TestMessage("How was your day?", "SAFE_NOISE", null, false, emptyList(), 6)
        )
        insertSenderWithMessages(mum, mumMessages)

        // Best friend - mostly safe
        val friend = TestSender(name = "Taylor", phone = "0456789012", photoUri = null)
        val friendMessages = listOf(
            TestMessage("Movie tonight? New Marvel is out!", "SAFE_LOGISTICS", null, true, emptyList(), 0),
            TestMessage("lol that meme was hilarious", "SAFE_NOISE", null, false, emptyList(), 1),
            TestMessage("Pick you up at 7", "SAFE_LOGISTICS", null, true, emptyList(), 1),
            TestMessage("Thanks for listening yesterday. Means a lot.", "SAFE_NOISE", null, false, emptyList(), 4),
            TestMessage("Brunch Saturday? That new cafe opened.", "SAFE_LOGISTICS", null, true, emptyList(), 7)
        )
        insertSenderWithMessages(friend, friendMessages)
    }

    private suspend fun seedBusinessSenders() {
        // Bank - alphanumeric sender
        val bank = TestSender(name = "MyBank", phone = "MYBANK", photoUri = null)
        val bankMessages = listOf(
            TestMessage("Your account balance is $1,234.56 as of today.", "SAFE_LOGISTICS", null, true, emptyList(), 0),
            TestMessage("Transaction alert: $50.00 at COFFEE SHOP.", "SAFE_LOGISTICS", null, true, emptyList(), 1),
            TestMessage("Your credit card payment of $500 is due March 20.", "SAFE_LOGISTICS", null, true, emptyList(), 3)
        )
        insertSenderWithMessages(bank, bankMessages)

        // Delivery
        val delivery = TestSender(name = "AusPost", phone = "AUSPOST", photoUri = null)
        val deliveryMessages = listOf(
            TestMessage("Your parcel is out for delivery today. Track: AP123456789AU", "SAFE_LOGISTICS", null, true, emptyList(), 0),
            TestMessage("Delivered! Left at front door.", "SAFE_LOGISTICS", null, true, emptyList(), 0)
        )
        insertSenderWithMessages(delivery, deliveryMessages)

        // Doctor
        val doctor = TestSender(name = "City Medical", phone = "0398765432", photoUri = null)
        val doctorMessages = listOf(
            TestMessage("Reminder: Your appointment is tomorrow at 10:30am with Dr. Smith.", "SAFE_LOGISTICS", null, true, emptyList(), 1),
            TestMessage("Your test results are ready. Please call to discuss.", "SAFE_LOGISTICS", null, true, emptyList(), 5)
        )
        insertSenderWithMessages(doctor, doctorMessages)
    }

    private suspend fun insertSenderWithMessages(sender: TestSender, messages: List<TestMessage>) {
        val normalizedPhone = phoneNumberNormalizer.normalize(sender.phone)
        val threadId = normalizedPhone.takeLast(10)
        val now = System.currentTimeMillis()

        // Calculate sender summary stats
        var filteredCount = 0
        var noiseCount = 0
        var toxicLogisticsCount = 0
        var criticismCount = 0
        var contemptCount = 0
        var defensivenessCount = 0
        var stonewallingCount = 0
        var totalToxicity = 0f
        var lastFiltered: Long? = null

        // Insert messages
        messages.forEachIndexed { index, testMsg ->
            val timestamp = now - (testMsg.daysAgo * 24 * 60 * 60 * 1000L) - (index * 60000) // Stagger by minutes
            val isFiltered = testMsg.category.startsWith("TOXIC")

            if (isFiltered) {
                filteredCount++
                lastFiltered = maxOf(lastFiltered ?: 0, timestamp)
            }
            when (testMsg.category) {
                "TOXIC_NOISE" -> noiseCount++
                "TOXIC_LOGISTICS" -> toxicLogisticsCount++
            }

            testMsg.horsemen.forEach { (type, confidence) ->
                totalToxicity += confidence
                when (type) {
                    "CRITICISM" -> criticismCount++
                    "CONTEMPT" -> contemptCount++
                    "DEFENSIVENESS" -> defensivenessCount++
                    "STONEWALLING" -> stonewallingCount++
                }
            }

            val messageEntity = MessageEntity(
                id = UUID.randomUUID().toString(),
                threadId = threadId,
                address = sender.phone,
                senderIdNormalized = normalizedPhone,
                direction = "INBOUND",
                timestamp = timestamp,
                isIncoming = true,
                originalContent = testMsg.body.toByteArray(Charsets.UTF_8),
                filteredContent = if (isFiltered) "[Filtered: ${testMsg.horsemen.firstOrNull()?.first ?: "toxic"} detected]" else null,
                isFiltered = isFiltered,
                toxicityScore = testMsg.horsemen.maxOfOrNull { it.second },
                classification = if (isFiltered) "HARMFUL" else "SAFE",
                horsemenDetected = if (testMsg.horsemen.isNotEmpty()) {
                    "[${testMsg.horsemen.joinToString(",") { "\"${it.first}\"" }}]"
                } else null,
                horsemenConfidences = if (testMsg.horsemen.isNotEmpty()) {
                    "{${testMsg.horsemen.joinToString(",") { "\"${it.first}\":${it.second}" }}}"
                } else null,
                analysisReasoning = testMsg.horsemen.firstOrNull()?.let { "${it.first} detected with ${(it.second * 100).toInt()}% confidence" },
                category = testMsg.category,
                severity = testMsg.severity,
                hasLogistics = testMsg.hasLogistics,
                engineVersion = "v1",
                analyzedAt = now,
                processingState = "SAFE",
                isSent = true,
                isRead = Random.nextBoolean(),
                isArchived = false
            )

            messageDao.insert(messageEntity)
        }

        // Insert thread
        val latestMessage = messages.minByOrNull { it.daysAgo }!!
        val latestTimestamp = now - (latestMessage.daysAgo * 24 * 60 * 60 * 1000L)

        val threadEntity = ThreadEntity(
            threadId = threadId,
            address = sender.phone,
            contactName = sender.name,
            contactPhotoUri = sender.photoUri,
            lastMessageId = null,
            lastMessageTime = latestTimestamp,
            lastMessagePreview = latestMessage.body.take(100),
            unreadCount = messages.count { !Random.nextBoolean() },
            messageCount = messages.size,
            toxicityLevel = if (filteredCount > 0) "HARMFUL" else "SAFE"
        )

        threadDao.insert(threadEntity)

        // Insert sender summary
        val senderSummary = SenderSummaryEntity(
            senderId = normalizedPhone,
            contactName = sender.name,
            contactPhotoUri = sender.photoUri,
            totalMessageCount = messages.size,
            filteredCount = filteredCount,
            noiseCount = noiseCount,
            toxicLogisticsCount = toxicLogisticsCount,
            criticismCount = criticismCount,
            contemptCount = contemptCount,
            defensivenessCount = defensivenessCount,
            stonewallingCount = stonewallingCount,
            totalToxicityScore = totalToxicity,
            lastMessageTimestamp = latestTimestamp,
            lastFilteredTimestamp = lastFiltered,
            engineVersion = "v1",
            createdAt = now,
            updatedAt = now
        )

        senderSummaryDao.insert(senderSummary)
    }

    private data class TestSender(
        val name: String,
        val phone: String,
        val photoUri: String?
    )

    private data class TestMessage(
        val body: String,
        val category: String, // SAFE_LOGISTICS, SAFE_NOISE, TOXIC_LOGISTICS, TOXIC_NOISE
        val severity: String?, // LOW, MEDIUM, HIGH, CRITICAL
        val hasLogistics: Boolean,
        val horsemen: List<Pair<String, Float>>, // type to confidence
        val daysAgo: Int
    )
}
