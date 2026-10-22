package com.minijarvis.ai

import kotlinx.coroutines.flow.Flow

/**
 * Metadata about the loaded model.
 */
data class ModelInfo(
    val name: String,
    val architecture: String,
    val quantization: String,
    val contextLength: Int,
    val ramUsageMb: Long,
    val isLoaded: Boolean
)

/**
 * Pluggable abstraction layer for on-device inference engines.
 * Enables zero-refactor swapping between Qwen, Llama, Gemma, or custom GGUF runtimes.
 */
interface LocalLLM {
    /**
     * Unique identifier for this inference engine implementation.
     */
    val engineName: String

    /**
     * Initializes and loads the model into RAM/VRAM.
     */
    suspend fun loadModel(modelPath: String, contextLength: Int = 2048): Result<ModelInfo>

    /**
     * Unloads model from memory to free resources on OPPO A3 Pro 5G.
     */
    suspend fun unloadModel()

    /**
     * Generates a complete response for a prompt synchronously within coroutine.
     */
    suspend fun generate(prompt: String): Result<String>

    /**
     * Generates tokens as a reactive stream for fast, low-latency UI updates.
     */
    fun generateStream(prompt: String): Flow<String>

    /**
     * Checks if the model is currently active and ready for inference.
     */
    fun isReady(): Boolean
}
