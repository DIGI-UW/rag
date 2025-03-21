package org.uwdigi.rag.config;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.uwdigi.rag.service.ModelFactory;
import org.uwdigi.rag.service.SqlDatabaseContentRetriever;
import org.uwdigi.rag.shared.Assistant;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
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

    @Value("${app.chatWindow.memory}")
    private int maxWindowChatMemory;

    private final ModelFactory modelFactory;

    public AppConfig(ModelFactory modelFactory) {
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
        log.info("Creating dynamic chat model via ModelFactory");
        return modelFactory.createModel();
    }

    @Bean
    public ContentRetriever sqlDatabaseContentRetriever(DataSource dataSource, ChatLanguageModel chatLanguageModel) {
        return SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatLanguageModel(chatLanguageModel)
                .modelFactory(modelFactory) // Pass ModelFactory for runtime model creation
                .build();
    }

    @Bean
    public Assistant assistant(ChatLanguageModel chatLanguageModel, ContentRetriever sqlDatabaseContentRetriever) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
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
