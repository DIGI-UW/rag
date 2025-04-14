package org.uwdigi.rag.config;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for AI model settings. This class maintains the current active model and all
 * API keys/URLs.
 */
@Configuration
@Getter
public class ModelConfig {
  private static final Logger log = LoggerFactory.getLogger(ModelConfig.class);

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
}
