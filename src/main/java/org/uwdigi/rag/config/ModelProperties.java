package org.uwdigi.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.uwdigi.rag.enums.ModelType;

@Component
@ConfigurationProperties(prefix = "ai.model")
public class ModelProperties {
    private ModelType activeModel = ModelType.GEMINI;
    private boolean fallbackEnabled = true;
    private ModelType fallbackModel = ModelType.LOCAL_AI;

    // Gemini properties
    private String geminiApiKey;
    private String geminiModelName;

    // Ollama properties
    private String ollamaBaseUrl;
    private String ollamaModelName;

    // LocalAI properties
    private String localAiBaseUrl;
    private String localAiModelName;
    private Double temperature;
    private Integer timeoutMinutes;

    // Getters and Setters
    public ModelType getActiveModel() {
        return activeModel;
    }

    public void setActiveModel(ModelType activeModel) {
        this.activeModel = activeModel;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    public void setFallbackEnabled(boolean fallbackEnabled) {
        this.fallbackEnabled = fallbackEnabled;
    }

    public ModelType getFallbackModel() {
        return fallbackModel;
    }

    public void setFallbackModel(ModelType fallbackModel) {
        this.fallbackModel = fallbackModel;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiModelName() {
        return geminiModelName;
    }

    public void setGeminiModelName(String geminiModelName) {
        this.geminiModelName = geminiModelName;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String getOllamaModelName() {
        return ollamaModelName;
    }

    public void setOllamaModelName(String ollamaModelName) {
        this.ollamaModelName = ollamaModelName;
    }

    public String getLocalAiBaseUrl() {
        return localAiBaseUrl;
    }

    public void setLocalAiBaseUrl(String localAiBaseUrl) {
        this.localAiBaseUrl = localAiBaseUrl;
    }

    public String getLocalAiModelName() {
        return localAiModelName;
    }

    public void setLocalAiModelName(String localAiModelName) {
        this.localAiModelName = localAiModelName;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(Integer timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }
}