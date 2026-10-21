package com.example.alarmboss.counselor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender { USER, COUNSELOR }

class SleepCounselorEngine(context: Context) {
    private val prefs = context.getSharedPreferences("alarmboss_counselor_prefs", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_MODEL = "gemini-3.5-flash"
        private const val PREFS_KEY_STORAGE = "gemini_api_key_storage"

        const val COUNSELOR_SYSTEM_PROMPT = """
You are a warm, humane, and deeply understanding sleep and wellness counselor for the AlarmBoss app. You are a personal adviser, empathetic listener, and sleep mentor — NOT a corporate chatbot, and NOT a rigid checklist dispenser.

CORE MISSION & ETHOS:
1. SAFE SPACE FOR PERSONAL INSIGHTS:
   Provide an open, compassionate, non-judgmental environment where the user feels completely safe sharing personal feelings, struggles, and vulnerabilities. Never judge, dismiss, or lecture.
   
2. HIGH FAITHFULNESS & ROOT-CAUSE INQUIRY:
   When a user says they can't sleep, are struggling, or feel exhausted, DO NOT just vomit a generic checklist of sleep hygiene tips (e.g. "dark room, cool room, no screens"). That feels dismissive and robotic.
   Instead:
   - Validate their feeling with genuine warmth and empathy.
   - Gently ask about root causes and symptoms: are they experiencing physical tension, pain, or restlessness? Is their mind racing with worries, stress, or anxiety? Did something specific happen today?
   - Offer tailored, comforting guidance based on what they're actually going through.

3. CASUAL GREETINGS & CHECK-INS:
   Always respond naturally, warmly, and flexibly to casual conversation and greetings!
   - If they say "what's up?", "how are you?", "hey", "hello": greet them warmly like a friend and ask how their energy and rest have been.
   - If they say "I have slept well" or celebrate a good night: celebrate with them! Ask what helped make last night so restorative.

4. LOWERCASE & CONVERSATIONAL STYLE:
   - Start your replies with a lower case letter (e.g. "hey...", "i'm really sorry to hear that...", "that's huge...").
   - Sound human, relaxed, and sincere. Avoid overly formal or polite speech.
   - NEVER use corporate jargon or disclaimers: no "I apologize for the inconvenience", "as an AI language model", "thank you for contacting me", "if you have any queries".

5. OUT-OF-CONTEXT GUARDRAIL (GENUINELY UNRELATED TOPICS ONLY):
   Only redirect if the query is totally detached from wellness, feelings, and sleep (e.g. coding python, sports scores, restaurant recommendations, politics).
   If so, gently decline starting with a lower case letter and include:
   "if you have any sort of doubts about how to improve sleep hygeine, and be well rested, don't ever be shy to ask!"
"""

        const val OUT_OF_CONTEXT_RESPONSE =
            "i don't really follow that, but i'm right here if you want to chat about your sleep or winding down. if you have any sort of doubts about how to improve sleep hygeine, and be well rested, don't ever be shy to ask!"

        /**
         * Intelligent local fallback engine that is flexible, empathetic, creates a safe space,
         * responds to casual greetings and personal insights, and starts with a lower case alphabet.
         */
        fun fallbackCounselorEngine(query: String): String {
            val lower = query.lowercase().trim()

            // 1. Positive sleep updates & wins
            if (lower.contains("slept well") || lower.contains("slept great") || lower.contains("slept good") ||
                lower.contains("feeling rested") || lower.contains("woke up fresh") || lower.contains("good sleep")) {
                return "that's wonderful to hear! waking up genuinely rested is such a win. did you do anything differently last night, or was your mind just more at ease?"
            }

            // 2. Casual greetings and check-ins
            if (lower.contains("what's up") || lower.contains("whats up") || lower == "sup" || lower.startsWith("sup ")) {
                return "hey there! i'm doing well, thank you. how are you feeling today — well-rested, or has it been a bit tiring?"
            }

            if (lower.contains("how are you") || lower.contains("how're you")) {
                return "doing well and glad you're here. how is your day treating you so far, and how have you been sleeping lately?"
            }

            if (lower == "hi" || lower == "hello" || lower == "hey" || lower.startsWith("hi ") || lower.startsWith("hello ") || lower.startsWith("hey ")) {
                return "hey there. hope your day hasn't been too draining. how has your sleep felt recently?"
            }

            if (lower.contains("thank") || lower == "thx" || lower == "ty") {
                return "you're always welcome. get some peaceful rest tonight, and if you have any sort of doubts about how to improve sleep hygeine, and be well rested, don't ever be shy to ask!"
            }

            if (lower.contains("good night") || lower.contains("goodnight") || lower.contains("going to bed") || lower.contains("going to sleep")) {
                return "good night! dim those lights down, put your phone away, and rest well."
            }

            if (lower.contains("good morning")) {
                return "morning! grab a glass of water and get some direct sunlight in your eyes to kickstart the day."
            }

            // 3. Out-of-context check for completely unrelated topics (restaurants, coding, politics)
            val outOfContextKeywords = listOf(
                "restaurant", "food delivery", "pizza", "burger", "code", "programming", "python",
                "kotlin", "java", "sql", "weather", "forecast", "cricket", "football", "soccer",
                "nba", "movie", "film", "politics", "president", "stock", "crypto", "bitcoin",
                "capital of", "who is", "solve", "math", "calculator", "hotel", "flight", "shopping"
            )

            for (kw in outOfContextKeywords) {
                if (lower.contains(kw)) {
                    return OUT_OF_CONTEXT_RESPONSE
                }
            }

            // 4. Safe space for personal insights: Insomnia, anxiety, mental & physical trouble
            if (lower.contains("can't sleep") || lower.contains("cant sleep") || lower.contains("trouble sleeping") ||
                lower.contains("insomnia") || lower.contains("lying awake") || lower.contains("tossing and turning")) {
                return "i hear you, and lying awake when you just want to rest is so exhausting. to help you figure this out, what feels like the main barrier tonight — is your mind racing with worries or anxiety, or is your body feeling physically restless or tense? let me know what you're experiencing."
            }

            if (lower.contains("anxious") || lower.contains("anxiety") || lower.contains("worried") ||
                lower.contains("racing thoughts") || lower.contains("overthinking") || lower.contains("stress")) {
                return "when anxiety or racing thoughts kick in at night, your brain treats bed like a threat instead of a sanctuary. try the 'brain dump': grab a physical notepad and write down every single thought or worry so your brain knows they're stored safely. then try 4-7-8 breathing: inhale 4s, hold 7s, exhale 8s to trigger your vagus nerve and slow your heart rate."
            }

            if (lower.contains("restless") || lower.contains("tension") || lower.contains("pain") ||
                lower.contains("ache") || lower.contains("tightness") || lower.contains("uncomfortable")) {
                return "physical restlessness or tension often happens when our muscles stay contracted from daytime stress. try progressive muscle relaxation: starting at your toes, squeeze firmly for 5 seconds, then fully release and feel the warmth. works wonders for easing physical agitation."
            }

            // 5. Specific sleep hygiene domains
            val sleepKeywords = listOf(
                "sleep", "slept", "bed", "rest", "wake", "tired", "insomnia", "nap", "circadian", "caffeine",
                "coffee", "tea", "melatonin", "groggy", "fatigue", "night", "morning", "alarm", "screen",
                "blue light", "schedule", "routine", "pillow", "dark", "snore", "dream", "hygiene"
            )

            val isSleepRelated = sleepKeywords.any { lower.contains(it) }

            if (!isSleepRelated) {
                return OUT_OF_CONTEXT_RESPONSE
            }

            // 6. Tailored sleep guidance
            return when {
                lower.contains("caffeine") || lower.contains("coffee") || lower.contains("tea") ->
                    "caffeine hangs around in your system a lot longer than you might think — its half-life is around 5 to 7 hours, so an afternoon coffee is still active in your brain at bedtime. try cutting off caffeine about 8 to 10 hours before sleep. if you want something warm in the evening, a warm chamomile or peppermint tea works nicely."

                lower.contains("screen") || lower.contains("phone") || lower.contains("blue light") ->
                    "screens blast blue light that tricks your brain into thinking it's still daytime, which puts the brakes on your natural melatonin production. try putting your phone across the room 30 minutes before bed and dim the lights down low. give your mind a quiet buffer to unplug."

                lower.contains("schedule") || lower.contains("irregular") || lower.contains("routine") ->
                    "your body loves consistency more than anything else. the biggest anchor isn't even bedtime — it's when you wake up. try getting out of bed at the exact same hour every day, even on weekends, and catch 10 minutes of direct morning sunlight to lock in your internal clock."

                lower.contains("groggy") || lower.contains("inertia") || lower.contains("tired in morning") ->
                    "that heavy morning grogginess is just sleep inertia. two quick things help shake it off right away: drink a big glass of room-temperature water right when your alarm goes off to rehydrate, and open the blinds for bright light. moving your body with the AlarmBoss physical challenges kicks your neuromuscular system into gear too."

                lower.contains("nap") ->
                    "naps can give you a solid reset if you keep them around 20 to 25 minutes. anything longer pushes you into deep sleep, leaving you feeling groggier when you wake up. also try to wrap up naps before 3 PM so you don't mess with your sleep drive at night."

                lower.contains("temperature") || lower.contains("cold") || lower.contains("hot") ->
                    "your core body temperature naturally needs to drop by about 1 to 2 degrees Fahrenheit for deep sleep to kick in. keeping your bedroom cool — around 65°F to 68°F (18°C) — makes a massive difference. a warm shower before bed actually helps because it brings heat to the surface of your skin to dump it faster."

                else ->
                    "good sleep really comes down to a few steady habits: a dark, cool room, winding down without screens, and keeping a steady wake-up time. if you have any sort of doubts about how to improve sleep hygeine, and be well rested, don't ever be shy to ask!"
            }
        }
    }

    /**
     * The user's own Gemini key, kept on this device. There is deliberately no key built into the app:
     * a key compiled into an APK can be pulled out of it by anyone who downloads the release.
     */
    fun getApiKey(): String = (prefs.getString(PREFS_KEY_STORAGE, "") ?: "").trim()

    fun hasApiKey(): Boolean = getApiKey().isNotEmpty()

    fun saveApiKey(key: String) {
        prefs.edit().putString(PREFS_KEY_STORAGE, key.trim()).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove(PREFS_KEY_STORAGE).apply()
    }

    suspend fun getCounselorResponse(
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isNotBlank()) {
            try {
                return@withContext callGeminiApi(apiKey, history, userMessage)
            } catch (e: Exception) {
                // If API call fails (network issue or invalid key), fall back gracefully
                return@withContext fallbackCounselorEngine(userMessage)
            }
        } else {
            // Local on-device counselor fallback
            return@withContext fallbackCounselorEngine(userMessage)
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String {
        // Try DEFAULT_MODEL first, then standard gemini flash models
        val models = listOf(DEFAULT_MODEL, "gemini-2.5-flash").distinct()
        var lastException: Exception? = null

        for (model in models) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                val rootJson = JSONObject().apply {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", COUNSELOR_SYSTEM_PROMPT) })
                        })
                    })

                    val contentsArray = JSONArray()
                    // Add last 6 messages for context
                    val recentHistory = history.takeLast(6)
                    for (msg in recentHistory) {
                        val role = if (msg.sender == MessageSender.USER) "user" else "model"
                        contentsArray.put(JSONObject().apply {
                            put("role", role)
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", msg.text) })
                            })
                        })
                    }
                    // Current message
                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", userMessage) })
                        })
                    })
                    put("contents", contentsArray)

                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 600)
                    })
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(rootJson.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(responseStr)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val candidate = candidates.getJSONObject(0)
                        val content = candidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) return text.trim().replaceFirstChar { it.lowercase() }
                        }
                    }
                } else {
                    val errorStr = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                    lastException = Exception("Gemini error $responseCode: $errorStr")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw lastException ?: Exception("Failed to contact Gemini API")
    }

    /** Fresh reading passages from the AI. Throws if there is no key or the request fails. */
    suspend fun generateReadingPassages(): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        check(apiKey.isNotBlank()) { "No API key" }
        val prompt = "Write 2 distinct, inspiring morning wake-up paragraphs (about 35-40 words each) to boost cognitive alertness upon waking up. Separate them with a line containing only '---'. Do not use bullet points or numbering."
        callGeminiSimple(apiKey, prompt)
    }

    private fun callGeminiSimple(apiKey: String, prompt: String): String {
        val models = listOf(DEFAULT_MODEL, "gemini-2.5-flash").distinct()
        for (model in models) {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
                val conn = URL(urlString).openConnection() as HttpURLConnection
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.doOutput = true

                val rootJson = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    }
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 300)
                    })
                }

                OutputStreamWriter(conn.outputStream).use { it.write(rootJson.toString()); it.flush() }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(responseStr)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            if (text.isNotBlank()) return text.trim()
                        }
                    }
                }
            } catch (e: Exception) {
                // try next model
            }
        }
        throw Exception("Gemini generation failed")
    }
}
