package com.example.alarmboss.counselor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepCounselorEngineTest {

    @Test
    fun `all counselor responses start with lowercase alphabet and avoid overly polite robotic phrasing`() {
        val testQueries = listOf(
            "what is the most popular restaurant",
            "thank you",
            "thanks so much",
            "hello",
            "what's up",
            "what's up? I have slept well",
            "I can't sleep at night.",
            "I'm feeling very anxious and stressed",
            "good night",
            "How does caffeine affect my deep sleep?",
            "Why do screens keep me awake at night?",
            "How do I fix my irregular sleep schedule?",
            "What should I do about morning grogginess?",
            "How long should I power nap?",
            "What temperature is best for my room?"
        )

        for (q in testQueries) {
            val response = SleepCounselorEngine.fallbackCounselorEngine(q)
            // Must start with a lower case alphabet
            assertTrue("Expected response for '$q' to start with lowercase: $response", response.first().isLowerCase())
            // Must not be overly polite or robotic
            assertTrue("Response should avoid 'I'm sorry': $response", !response.contains("I'm sorry", ignoreCase = true))
            assertTrue("Response should avoid corporate phrases: $response", !response.contains("if you have any queries", ignoreCase = true))
            assertTrue("Response should avoid AI disclaimers: $response", !response.contains("as an AI language model", ignoreCase = true))
        }
    }

    @Test
    fun `casual greetings and sleep wins are acknowledged with warmth instead of rejection`() {
        val sleepWin = SleepCounselorEngine.fallbackCounselorEngine("what's up? I have slept well")
        assertTrue(sleepWin != SleepCounselorEngine.OUT_OF_CONTEXT_RESPONSE)
        assertTrue(sleepWin.contains("wonderful to hear") || sleepWin.contains("rested"))
        assertTrue(sleepWin.first().isLowerCase())

        val whatsUp = SleepCounselorEngine.fallbackCounselorEngine("what's up")
        assertTrue(whatsUp != SleepCounselorEngine.OUT_OF_CONTEXT_RESPONSE)
        assertTrue(whatsUp.first().isLowerCase())
    }

    @Test
    fun `insomnia inquiries provide safe space and inquire about anxiety and physical symptoms`() {
        val cantSleep = SleepCounselorEngine.fallbackCounselorEngine("I can't sleep at night.")
        assertTrue(cantSleep != SleepCounselorEngine.OUT_OF_CONTEXT_RESPONSE)
        // Must provide safe space and ask about anxiety or physical restlessness
        assertTrue(cantSleep.contains("anxiety", ignoreCase = true) || cantSleep.contains("worries", ignoreCase = true))
        assertTrue(cantSleep.contains("restless", ignoreCase = true) || cantSleep.contains("tense", ignoreCase = true))
        assertTrue(cantSleep.first().isLowerCase())
    }

    @Test
    fun `out of context questions receive gentle human decline with exact required phrasing`() {
        val outOfContextQuestions = listOf(
            "what is the most popular restaurant",
            "where can I get pizza tonight?",
            "how do I write a binary search in python?",
            "who is the president of france?",
            "what is the weather like in tokyo?",
            "who won the football match?"
        )

        for (q in outOfContextQuestions) {
            val response = SleepCounselorEngine.fallbackCounselorEngine(q)
            assertEquals(SleepCounselorEngine.OUT_OF_CONTEXT_RESPONSE, response)
            assertTrue(response.startsWith("i "))
            assertTrue(response.contains("if you have any sort of doubts about how to improve sleep hygeine, and be well rested, don't ever be shy to ask!"))
        }
    }

    @Test
    fun `pleasantries like thank you and hello receive warm understanding replies in lowercase`() {
        val thanks = SleepCounselorEngine.fallbackCounselorEngine("thank you")
        assertTrue(thanks.startsWith("you're always welcome"))
        assertTrue(thanks.first().isLowerCase())
        assertTrue(thanks.contains("if you have any sort of doubts about how to improve sleep hygeine, and be well rested, don't ever be shy to ask!"))

        val hello = SleepCounselorEngine.fallbackCounselorEngine("hello")
        assertTrue(hello.startsWith("hey there"))
        assertTrue(hello.first().isLowerCase())

        val goodnight = SleepCounselorEngine.fallbackCounselorEngine("good night")
        assertTrue(goodnight.startsWith("good night"))
        assertTrue(goodnight.first().isLowerCase())
    }

    @Test
    fun `sleep hygiene questions receive warm advice without corporate jargon`() {
        val sleepQuestions = listOf(
            "How does caffeine affect my deep sleep?",
            "Why do screens keep me awake at night?",
            "How do I fix my irregular sleep schedule?",
            "What should I do about morning grogginess?",
            "How long should I power nap?"
        )

        for (q in sleepQuestions) {
            val response = SleepCounselorEngine.fallbackCounselorEngine(q)
            // Must not be the out-of-context rejection
            assertTrue(response != SleepCounselorEngine.OUT_OF_CONTEXT_RESPONSE)
            // Starts with lowercase
            assertTrue(response.first().isLowerCase())
            // Must not contain corporate hollow phrases
            assertTrue(!response.contains("if you have any queries", ignoreCase = true))
            assertTrue(!response.contains("as an AI language model", ignoreCase = true))
            assertTrue(!response.contains("feel free to reach out", ignoreCase = true))
        }
    }
}
