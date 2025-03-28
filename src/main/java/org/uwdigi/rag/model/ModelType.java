package org.uwdigi.rag.model;

/**
 * Enum representing different AI model types that can be used in the
 * application.
 * This allows for runtime switching between different models.
 */
public enum ModelType {
    GEMINI,
    CLAUDE,
    DEEPSEEK,
    OLLAMA,
    LOCAL_AI
}