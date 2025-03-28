package org.uwdigi.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.uwdigi.rag.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration class for AI model settings.
 * This class maintains the current active model and all API keys/URLs.
 */
@Configuration
public class ModelConfig {
    private static final Logger log = LoggerFactory.getLogger(ModelConfig.class);

    @Value("${app.model.active:GEMINI}")
    private ModelType activeModel;

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.claude.api-key:}")
    private String claudeApiKey;
    
    @Value("${app.openai.api-key:}")
    private String openaiApiKey;

    @Value("${app.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${app.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model-name}")
    private String ollamaModelName;

    @Value("${app.local-ai.base-url}")
    private String localAiBaseUrl;

    @Value("${app.local-ai.model-name}")
    private String localAiModelName;

    // Getters
    public ModelType getActiveModel() {
        return activeModel;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public String getClaudeApiKey() {
        return claudeApiKey;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }


    public String getDeepseekApiKey() {
        return deepseekApiKey;
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public String getOllamaModelName() {
        return ollamaModelName;
    }

    public String getLocalAiBaseUrl() {
        return localAiBaseUrl;
    }

    public String getLocalAiModelName() {
        return localAiModelName;
    }

    // Setters
    public void setActiveModel(ModelType activeModel) {
        log.info("Changing active model from {} to {}", this.activeModel, activeModel);
        this.activeModel = activeModel;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

     public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public void setClaudeApiKey(String claudeApiKey) {
        this.claudeApiKey = claudeApiKey;
    }

    public void setDeepseekApiKey(String deepseekApiKey) {
        this.deepseekApiKey = deepseekApiKey;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public void setOllamaModelName(String ollamaModelName) {
        this.ollamaModelName = ollamaModelName;
    }

    public void setLocalAiBaseUrl(String localAiBaseUrl) {
        this.localAiBaseUrl = localAiBaseUrl;
    }

    public void setLocalAiModelName(String localAiModelName) {
        this.localAiModelName = localAiModelName;
    }
}