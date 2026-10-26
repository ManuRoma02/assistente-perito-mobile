package com.example.assistenteperitomobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReasoningModelResolutionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createDummyModelFile(dir: File, name: String, sizeBytes: Long = 15_000_000L): File {
        val f = File(dir, name)
        java.io.RandomAccessFile(f, "rw").use { raf ->
            raf.setLength(sizeBytes)
        }
        return f
    }

    @Test
    fun `test selected 2b model present resolves without fallback`() {
        val modelsDir = tempFolder.newFolder("models")
        createDummyModelFile(modelsDir, "Qwen3.5-2B-GPT-5.1-HighIQ-INSTRUCT.i1-Q4_K_M.gguf")

        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(reasoningModelId = "qwen35_2b_gpt51_highiq_q4km")
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        assertNotNull(res.resolvedFile)
        assertFalse(res.isFallback)
        assertEquals("qwen35_2b_gpt51_highiq_q4km", res.effectiveModelId)
        assertEquals("Qwen 3.5 2B (Q4_K_M)", res.effectiveLabel)
    }

    @Test
    fun `test selected 4b model present resolves without fallback`() {
        val modelsDir = tempFolder.newFolder("models")
        createDummyModelFile(modelsDir, "Qwen_Qwen3.5-4B-Q4_K_M.gguf")

        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(reasoningModelId = "qwen35_4b_instruct_q4km")
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        assertNotNull(res.resolvedFile)
        assertFalse(res.isFallback)
        assertEquals("qwen35_4b_instruct_q4km", res.effectiveModelId)
        assertEquals("Qwen 3.5 4B Potenziato", res.effectiveLabel)
    }

    @Test
    fun `test selected 4b model absent falls back to 2b with explicit fallback label`() {
        val modelsDir = tempFolder.newFolder("models")
        createDummyModelFile(modelsDir, "Qwen3.5-2B-GPT-5.1-HighIQ-INSTRUCT.i1-Q4_K_M.gguf")

        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(reasoningModelId = "qwen35_4b_instruct_q4km")
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        assertNotNull(res.resolvedFile)
        assertTrue(res.isFallback)
        assertEquals("qwen35_2b_gpt51_highiq_q4km", res.effectiveModelId)
        assertEquals("Qwen 3.5 2B (Q4_K_M) [Fallback]", res.effectiveLabel)
    }

    @Test
    fun `test selected 2b model absent falls back to 4b with explicit fallback label`() {
        val modelsDir = tempFolder.newFolder("models")
        createDummyModelFile(modelsDir, "Qwen_Qwen3.5-4B-Q4_K_M.gguf")

        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(reasoningModelId = "qwen35_2b_gpt51_highiq_q4km")
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        assertNotNull(res.resolvedFile)
        assertTrue(res.isFallback)
        assertEquals("qwen35_4b_instruct_q4km", res.effectiveModelId)
        assertEquals("Qwen 3.5 4B Potenziato [Fallback]", res.effectiveLabel)
    }

    @Test
    fun `test no model present returns null file and explicit label`() {
        val modelsDir = tempFolder.newFolder("models")
        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(reasoningModelId = "qwen35_4b_instruct_q4km")
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        assertNull(res.resolvedFile)
        assertFalse(res.isFallback)
        assertNull(res.effectiveModelId)
        assertEquals("Nessun modello disponibile", res.effectiveLabel)
    }

    @Test
    fun `test rizzo flow in models dir is not resolved as reasoning model`() {
        val modelsDir = tempFolder.newFolder("models")
        // Rizzo Flow is a decision/classification model, not a reasoning model
        createDummyModelFile(modelsDir, "spark-x2.5-1.7b-rizzo-flow-lora-q4_k_m.gguf")

        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(reasoningModelId = "qwen35_2b_gpt51_highiq_q4km")
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        // Must NOT pick Rizzo Flow as fallback
        assertNull(res.resolvedFile)
        assertFalse(res.isFallback)
        assertNull(res.effectiveModelId)
        assertEquals("Nessun modello disponibile", res.effectiveLabel)
    }

    @Test
    fun `test rizzo flow configured in mediaPipeModelPath is rejected for reasoning`() {
        val modelsDir = tempFolder.newFolder("models")
        val rizzoFile = createDummyModelFile(modelsDir, "spark-x2.5-1.7b-rizzo-flow-lora-q4_k_m.gguf")

        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(
            reasoningModelId = "qwen35_2b_gpt51_highiq_q4km",
            mediaPipeModelPath = rizzoFile.absolutePath
        )
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        assertNull(res.resolvedFile)
        assertFalse(res.isFallback)
        assertNull(res.effectiveModelId)
        assertEquals("Nessun modello disponibile", res.effectiveLabel)
    }

    @Test
    fun `test valid custom non-classifier model in mediaPipeModelPath is resolved`() {
        val modelsDir = tempFolder.newFolder("models")
        val customModel = createDummyModelFile(modelsDir, "deepseek-r1-distill-qwen-1.5b-q4_k_m.gguf")

        val engine = PeritoAiEngine(context = null)
        val config = AiEngineConfig(
            reasoningModelId = "qwen35_2b_gpt51_highiq_q4km",
            mediaPipeModelPath = customModel.absolutePath
        )
        val res = engine.resolveReasoningModel(config, overrideModelsDir = modelsDir)

        assertNotNull(res.resolvedFile)
        assertTrue(res.isFallback)
        assertEquals(customModel.absolutePath, res.resolvedFile?.absolutePath)
        assertEquals("deepseek-r1-distill-qwen-1.5b-q4_k_m.gguf [Personalizzato]", res.effectiveLabel)
    }
}
