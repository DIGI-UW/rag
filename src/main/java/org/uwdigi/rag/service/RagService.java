package org.uwdigi.rag.service;

import org.springframework.stereotype.Service;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for handling RAG queries directly using the ModelFactory.
 * This allows for dynamic model selection at runtime.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private final ModelFactory modelFactory;

    public RagService(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    /**
     * Process a general query using the current active model.
     * 
     * @param query The query to process
     * @return The response from the model
     */
    public String processQuery(String query) {
        log.debug("Processing query: {}", query);
        ChatLanguageModel model = modelFactory.createModel();
        return model.chat(UserMessage.from(query)).aiMessage().text();
    }

    /**
     * Generate an SQL query from a natural language query using the current active
     * model.
     * 
     * @param naturalLanguageQuery The natural language query
     * @return The generated SQL query
     */
    public String generateSqlQuery(String naturalLanguageQuery) {
        log.debug("Generating SQL for query: {}", naturalLanguageQuery);
        ChatLanguageModel model = modelFactory.createModel();
        String prompt = String.format(
                "Convert the following natural language query to SQL. Only return the SQL query, no explanations:\n%s",
                naturalLanguageQuery);
        return model.chat(UserMessage.from(prompt)).aiMessage().text();
    }
}