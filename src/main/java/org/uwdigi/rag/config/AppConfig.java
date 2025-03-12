package org.uwdigi.rag.config;

import java.time.Duration;

import javax.sql.DataSource;

import org.mariadb.jdbc.MariaDbDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// import org.uwdigi.rag.service.SqlDatabaseContentRetriever;
import org.uwdigi.rag.shared.Assistant;

import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;

@Configuration
public class AppConfig {

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

    @Bean
    public DataSource dataSource() {
        MariaDbDataSource dataSource = new MariaDbDataSource();
        try {
            dataSource.setUrl(dbUrl);
            dataSource.setUser(dbUser);
            dataSource.setPassword(dbPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure datasource", e);
        }
        return dataSource;
    }

    @Bean
    public ChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-2.0-flash")
                .logRequestsAndResponses(true)
                .build();
    }

    @Bean
    public ChatLanguageModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    public ChatLanguageModel localAiChatModel() {
        return LocalAiChatModel.builder()
                .baseUrl(localAiBaseUrl)
                .modelName(localAiModelName)
                .logRequests(true)
                .logResponses(true)
                .temperature(0.0)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    public ContentRetriever sqlDatabaseContentRetriever(DataSource dataSource, ChatLanguageModel geminiChatModel) {
        return SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .sqlDialect("MySQL")
                .chatLanguageModel(geminiChatModel)
                .build();
    }

    @Bean
    public Assistant assistant(ChatLanguageModel geminiChatModel, ContentRetriever sqlDatabaseContentRetriever) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(geminiChatModel)
                .contentRetriever(sqlDatabaseContentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}