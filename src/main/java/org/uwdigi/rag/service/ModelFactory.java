package org.uwdigi.rag.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.uwdigi.rag.config.ModelProperties;
import org.uwdigi.rag.enums.ModelType;
import org.uwdigi.rag.exception.ModelInitializationException;

import java.time.Duration;

@Service
public class ModelFactory {
    private static final Logger log = LoggerFactory.getLogger(ModelFactory.class);
    private final ModelProperties properties;

    public ModelFactory(ModelProperties properties) {
        this.properties = properties;
    }

    public ChatLanguageModel createModel(ModelType modelType) {
        log.info("Creating model of type: {}", modelType);
        try {
            return switch (modelType) {
                case GEMINI -> createGeminiModel();
                case OLLAMA -> createOllamaModel();
                case LOCAL_AI -> createLocalAiModel();
                default -> throw new ModelInitializationException("Unsupported model type: " + modelType);
            };
        } catch (Exception e) {
            String errorMessage = String.format("Failed to create model of type %s: %s",
                    modelType, e.getMessage());
            log.error(errorMessage, e);
            throw new ModelInitializationException(errorMessage, e);
        }
    }

    private ChatLanguageModel createGeminiModel() {
        log.info("Initializing Gemini Chat Model...");
        try {
            ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                    .apiKey(properties.getGeminiApiKey())
                    .modelName(properties.getGeminiModelName())
                    .logRequestsAndResponses(true)
                    .build();
            log.info("Gemini Chat Model initialized successfully");
            return model;
        } catch (Exception e) {
            throw new ModelInitializationException("Failed to initialize Gemini model", e);
        }
    }

    private ChatLanguageModel createOllamaModel() {
        log.info("Initializing Ollama Chat Model...");
        try {
            ChatLanguageModel model = OllamaChatModel.builder()
                    .baseUrl(properties.getOllamaBaseUrl())
                    .modelName(properties.getOllamaModelName())
                    .logRequests(true)
                    .logResponses(true)
                    .timeout(Duration.ofMinutes(properties.getTimeoutMinutes()))
                    .build();
            log.info("Ollama Chat Model initialized successfully");
            return model;
        } catch (Exception e) {
            throw new ModelInitializationException("Failed to initialize Ollama model", e);
        }
    }

    private ChatLanguageModel createLocalAiModel() {
        log.info("Initializing Local AI Chat Model...");
        try {
            ChatLanguageModel model = LocalAiChatModel.builder()
                    .baseUrl(properties.getLocalAiBaseUrl())
                    .modelName(properties.getLocalAiModelName())
                    .logRequests(true)
                    .logResponses(true)
                    .temperature(properties.getTemperature())
                    .timeout(Duration.ofMinutes(properties.getTimeoutMinutes()))
                    .build();
            log.info("Local AI Chat Model initialized successfully");
            return model;
        } catch (Exception e) {
            throw new ModelInitializationException("Failed to initialize LocalAI model", e);
        }
    }
}