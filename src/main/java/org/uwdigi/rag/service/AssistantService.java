package org.uwdigi.rag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uwdigi.rag.shared.Assistant;
import org.uwdigi.rag.shared.QueryResponse;

/**
 * Service for handling user queries using AI assistants. This service integrates both the standard
 * Assistant and RagService for SQL generation.
 */
@Service
public class AssistantService {

  private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
  private final Assistant assistant;
  private final DataSource dataSource;
  private final EmbeddingModel embeddingModel;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final ModelFactory modelFactory;
  private final ChatLanguageModel ollamaChatModel;
  private String response;
  private String sqlRun;
  private String[] schemaType;

  @Autowired
  public AssistantService(
      Assistant assistant,
      String[] schemaType,
      DataSource dataSource,
      EmbeddingStore<TextSegment> embeddingStore,
      EmbeddingModel embeddingModel,
      ModelFactory modelFactory,
      @Qualifier("ollamaChatLanguageModel") ChatLanguageModel ollamaChatModel) {
    this.assistant = assistant;
    this.dataSource = dataSource;
    this.modelFactory = modelFactory;
    this.embeddingModel = embeddingModel;
    this.embeddingStore = embeddingStore;
    this.ollamaChatModel = ollamaChatModel;
    this.response = "Unexpected Error occured";
    this.sqlRun = "Unexpected Error occured";
    this.schemaType = schemaType;
  }

  public void updateSqlRun(String sqlRun) {
    log.debug("Updating SQL Query run on DB: {}", sqlRun);
    this.sqlRun = sqlRun;
  }

  public String getSqlRun() {
    return this.sqlRun;
  }

  public void updateResponse(String response) {
    log.debug("Updating response to: {}", response);
    this.response = response;
  }

  public String getResponse() {
    return this.response;
  }

  /**
   * Process a query using the appropriate service based on context. First attempts SQL generation,
   * then falls back to general assistant.
   *
   * @param query The user query
   * @return The response
   */
  public QueryResponse processQuery(String query, String modelName) {
    log.debug("Processing with model: {}", modelName);

    ChatLanguageModel chatLanguageModel = this.modelFactory.createModel(modelName);

    ContentRetriever contentRetriever =
        SqlDatabaseContentRetriever.builder()
            .dataSource(dataSource)
            .chatLanguageModel(chatLanguageModel)
            .ollamaChatModel(ollamaChatModel)
            .embeddingModel(embeddingModel)
            .embeddingStore(embeddingStore)
            .schemaType(schemaType)
            .assistantService(this)
            .build();
    log.debug("Processing query through AssistantService: {}", query);
    String answer =
        AiServices.builder(Assistant.class)
            .chatLanguageModel(chatLanguageModel)
            .contentRetriever(contentRetriever)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build()
            .answer(query);
    log.debug("The Cloud AI answer is : {}", answer);
    log.debug("The Local AI answer is : {}", this.response);
    log.debug("The SQL query run is : {}", this.sqlRun);

    return new QueryResponse(this.response, this.sqlRun);
  }
}
