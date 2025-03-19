package org.uwdigi.rag.config;

import java.time.Duration;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.hive.jdbc.HiveDataSource;
import org.mariadb.jdbc.MariaDbDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.uwdigi.rag.service.SqlDatabaseContentRetriever;
import org.uwdigi.rag.shared.Assistant;

//import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uwdigi.rag.service.ModelFactory;
// import org.uwdigi.rag.exception.ModelInitializationException;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.gemini.api-key}")
    private String geminiApiKey;

    @Value("${app.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.model-name}")
    private String ollamaModelName;

    @Value("${app.local-ai.base-url}")
    private String localAiBaseUrl;

    @Value("${app.local-ai.model-name}")
    private String localAiModelName;

    @Value("${app.chatWindow.memory}")
    private int maxWindowChatMemory;

    private final ModelProperties modelProperties;
    private final ModelFactory modelFactory;

    public AppConfig(ModelProperties modelProperties, ModelFactory modelFactory) {
        this.modelProperties = modelProperties;
        this.modelFactory = modelFactory;
    }

    @Bean
    public DataSource dataSource() {
        BasicDataSource dataSource = new BasicDataSource();
        try {
            dataSource.setUrl(dbUrl);
            dataSource.setUsername(dbUser);
            dataSource.setDriverClassName("org.apache.hive.jdbc.HiveDriver");
            dataSource.setPassword(dbPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure datasource", e);
        }
        return dataSource;
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("Initializing chat language model with type: {}",
                modelProperties.getActiveModel());

        try {
            return modelFactory.createModel(modelProperties.getActiveModel());
        } catch (Exception e) {
            if (modelProperties.isFallbackEnabled()) {
                log.warn("Failed to initialize primary model. Falling back to: {}",
                        modelProperties.getFallbackModel());
                try {
                    return modelFactory.createModel(modelProperties.getFallbackModel());
                } catch (Exception fallbackException) {
                    throw new ModelInitializationException(
                            "Both primary and fallback model initialization failed",
                            fallbackException);
                }
            }
            throw new ModelInitializationException(
                    "Model initialization failed and no fallback is configured", e);
        }
    }

    @Bean
    public ContentRetriever sqlDatabaseContentRetriever(DataSource dataSource, ChatLanguageModel geminiChatModel) {
        return SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatLanguageModel(geminiChatModel)
                .build();
    }

    @Bean
    public Assistant assistant(ChatLanguageModel geminiChatModel, ContentRetriever sqlDatabaseContentRetriever) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(geminiChatModel)
                .contentRetriever(sqlDatabaseContentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(maxWindowChatMemory))
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public JdbcMappingContext jdbcMappingContext() {
        return new JdbcMappingContext();
    }
}

// Custom exception for model initialization errors
class ModelInitializationException extends RuntimeException {
    public ModelInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
