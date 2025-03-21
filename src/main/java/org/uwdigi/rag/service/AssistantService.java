package org.uwdigi.rag.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uwdigi.rag.shared.Assistant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for handling user queries using AI assistants.
 * This service integrates both the standard Assistant and RagService for SQL
 * generation.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
    private final Assistant assistant;
    private final RagService ragService;

    @Autowired
    public AssistantService(Assistant assistant, RagService ragService) {
        this.assistant = assistant;
        this.ragService = ragService;
    }

    /**
     * Process a query using the appropriate service based on context.
     * First attempts SQL generation, then falls back to general assistant.
     * 
     * @param query The user query
     * @return The response
     */
    public String processQuery(String query) {
        log.debug("Processing query through AssistantService: {}", query);

        // First try to generate SQL using RagService if query seems SQL-related
        if (isSqlQuery(query)) {
            try {
                log.debug("Query appears to be SQL-related, attempting SQL generation");
                String sqlQuery = ragService.generateSqlQuery(query);
                if (sqlQuery != null && !sqlQuery.trim().isEmpty()) {
                    log.debug("Successfully generated SQL: {}", sqlQuery);
                    return sqlQuery;
                }
            } catch (Exception e) {
                log.warn("Failed to generate SQL, falling back to assistant: {}", e.getMessage());
            }
        }

        // Fall back to general assistant
        log.debug("Using general assistant for query");
        return assistant.answer(query);
    }

    /**
     * Simple heuristic to determine if a query is likely SQL-related.
     * This could be made more sophisticated with NLP techniques.
     */
    private boolean isSqlQuery(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("database") ||
                lowerQuery.contains("data") ||
                lowerQuery.contains("sql") ||
                lowerQuery.contains("table") ||
                lowerQuery.contains("query") ||
                lowerQuery.contains("select") ||
                lowerQuery.contains("find") ||
                lowerQuery.contains("show me") ||
                lowerQuery.contains("list") ||
                lowerQuery.contains("count");
    }
}