package org.uwdigi.rag.config;

import java.time.Duration;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;


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
    @Bean
    public DataSource dataSource() {
        MariaDbDataSource dataSource = new MariaDbDataSource();
        try {
            dataSource.setUrl(dbUrl);
            dataSource.setUser(dbUser);
            dataSource.setPassword(dbPassword);

        log.info("Initializing Datasource at " + dbUrl + " " + dbUser + " "+dbPassword );

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure datasource", e);
        }
        return dataSource;
    }

/*     @Bean
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
    } */

    @Bean
    public ChatLanguageModel geminiChatModel() {
        log.info("Initializing Gemini Chat Model...");
        try {
            ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName("gemini-2.0-flash")
                    .logRequestsAndResponses(true)
                    .build();
            log.info("Gemini Chat Model initialized successfully.");
            return model;
        } catch (Exception e) {
            log.error("Failed to initialize Gemini Chat Model: {}", e.getMessage(), e);
            throw new ModelInitializationException("Gemini Chat Model initialization failed", e);
        }
    }

    // @Bean
    // public ChatLanguageModel ollamaChatModel() {
    //     log.info("Initializing Ollama Chat Model...");
    //     try {
    //         ChatLanguageModel model = OllamaChatModel.builder()
    //                 .baseUrl(ollamaBaseUrl)
    //                 .modelName(ollamaModelName)
    //                 .logRequests(true)
    //                 .logResponses(true)
    //                 .timeout(Duration.ofMinutes(5))
    //                 .build();
    //         log.info("Ollama Chat Model initialized successfully.");
    //         return model;
    //     } catch (Exception e) {
    //         log.error("Failed to initialize Ollama Chat Model: {}", e.getMessage(), e);
    //         throw new ModelInitializationException("Ollama Chat Model initialization failed", e);
    //     }
    // }
    @Bean(name = "ollamaChatLanguageModel")
    public ChatLanguageModel ollamaChatModel() {
    log.info("Initializing Ollama Chat Model...");
    try {
        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofMinutes(5))
                .build();
        log.info("Ollama Chat Model initialized successfully.");
        return model;
    } catch (Exception e) {
        log.error("Failed to initialize Ollama Chat Model: {}", e.getMessage(), e);
        throw new ModelInitializationException("Ollama Chat Model initialization failed", e);
    }
}


    @Bean
    public ChatLanguageModel localAiChatModel() {
        log.info("Initializing Local AI Chat Model...");
        try {
            ChatLanguageModel model = LocalAiChatModel.builder()
                    .baseUrl(localAiBaseUrl)
                    .modelName(localAiModelName)
                    .logRequests(true)
                    .logResponses(true)
                    .temperature(0.0)
                    .timeout(Duration.ofMinutes(5))
                    .build();
            log.info("Local AI Chat Model initialized successfully.");
            return model;
        } catch (Exception e) {
            log.error("Failed to initialize Local AI Chat Model: {}", e.getMessage(), e);
            throw new ModelInitializationException("Local AI Chat Model initialization failed", e);
        }
    }

    @Bean
    public ContentRetriever sqlDatabaseContentRetriever(DataSource dataSource, ChatLanguageModel geminiChatModel,
            @Qualifier("ollamaChatLanguageModel") ChatLanguageModel ollamaChatModel) {

        return SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatLanguageModel(geminiChatModel)
                .ollamaChatModel(ollamaChatModel)
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
