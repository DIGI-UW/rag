package org.uwdigi.rag.service;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.uwdigi.rag.config.ModelConfig;

/**
 * Factory service class for running the appropriate function based on the currently active model
 * type.
 */
@Service
public class ModelFactory {

  private static final Logger log = LoggerFactory.getLogger(ModelFactory.class);
  private final ModelConfig modelConfig;

  public ModelFactory(ModelConfig modelConfig) {
    this.modelConfig = modelConfig;
  }

  /** Creates an instance of ChatLanguageModel based on the currently active model type. */
  public ChatLanguageModel createModel(String modelName) {
    try {
      switch (modelName) {
        case "GEMINI":
          return createGeminiModel();
        case "CLAUDE":
          return createClaudeModel();
        case "OPENAI":
          return createOpenAiChatModel();
        case "OLLAMA":
          return createOllamaModel();
        case "LOCAL_AI":
          return createLocalAiModel();
        default:
          return createGeminiModel();
      }
    } catch (Exception e) {
      log.error("Failed to create model: {}", e.getMessage(), e);
      throw new ModelInitializationException("Failed to initialize model", e);
    }
  }

  private ChatLanguageModel createGeminiModel() {
    log.info("Initializing Gemini Chat Model...");
    return GoogleAiGeminiChatModel.builder()
        .apiKey(modelConfig.getGeminiApiKey())
        .modelName("gemini-2.0-flash")
        .logRequestsAndResponses(true)
        .timeout(Duration.ofMinutes(2))
        .build();
  }

  private ChatLanguageModel createClaudeModel() {
    log.info("Initializing Claude Chat Model...");
    return AnthropicChatModel.builder()
        .apiKey(modelConfig.getClaudeApiKey())
        .modelName("claude-3-haiku-20240307")
        .timeout(Duration.ofMinutes(2))
        .build();
  }

  private ChatLanguageModel createOpenAiChatModel() {
    log.info("Initializing OpenAI Chat Model...");
    return OpenAiChatModel.builder()
        .apiKey(modelConfig.getOpenaiApiKey())
        .modelName("GPT_4_O_MINI")
        .logRequests(true)
        .logResponses(true)
        .timeout(Duration.ofMinutes(2))
        .build();
  }

  private ChatLanguageModel createOllamaModel() {
    log.info("Initializing Ollama Chat Model...");
    return OllamaChatModel.builder()
        .baseUrl(modelConfig.getOllamaBaseUrl())
        .modelName(modelConfig.getOllamaModelName())
        .logRequests(true)
        .logResponses(true)
        .timeout(Duration.ofMinutes(5))
        .build();
  }

  private ChatLanguageModel createLocalAiModel() {
    log.info("Initializing Local AI Chat Model...");
    return LocalAiChatModel.builder()
        .baseUrl(modelConfig.getLocalAiBaseUrl())
        .modelName(modelConfig.getLocalAiModelName())
        .logRequests(true)
        .logResponses(true)
        .temperature(0.0)
        .timeout(Duration.ofMinutes(5))
        .build();
  }
}

/** Custom exception for model initialization errors */
class ModelInitializationException extends RuntimeException {
  public ModelInitializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
