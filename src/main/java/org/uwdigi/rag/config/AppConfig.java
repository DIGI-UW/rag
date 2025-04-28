package org.uwdigi.rag.config;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.uwdigi.rag.service.SqlDatabaseContentRetriever;
import org.uwdigi.rag.shared.Assistant;

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

  @Value("${app.openai.api-key}")
  private String openaiApiKey;

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

  @Value("${app.db.schema.type}")
  private String appDbSchemaType;

  @Value("${app.pgvector.host}")
  private String pgVectorHost;

  @Value("${app.pgvector.port}")
  private Integer pgVectorPort;

  @Value("${app.pgvector.database}")
  private String pgVectorDatabase;

  @Value("${app.pgvector.user}")
  private String pgVectorUser;

  @Value("${app.pgvector.password}")
  private String pgVectorPassword;

  @Value("${app.pgvector.table}")
  private String pgVectorTable;

  @Value("${spring.datasource.type}")
  private String datasourceType;

  private final FhirDbConfig fhirDbConfig;

  public AppConfig(FhirDbConfig fhirDbConfig) {
    this.fhirDbConfig = fhirDbConfig;
  }

  @Bean
  public String[] schemaType() {
    return appDbSchemaType.split(",");
  }

  @Bean
  public DataSource dataSource() {
    try {
      log.info("Initializing DataSource with URL: {}", dbUrl);

      BasicDataSource defaultDataSource = new BasicDataSource();
      defaultDataSource.setUrl(dbUrl);
      defaultDataSource.setUsername(dbUser);
      defaultDataSource.setPassword(dbPassword);
      defaultDataSource.setDriverClassName(determineDriverClassNameFromUrl(dbUrl));

      return defaultDataSource;
    } catch (Exception e) {
      log.error("Failed to configure datasource", e);
      throw new RuntimeException("Failed to configure datasource", e);
    }
  }

  private String determineDriverClassNameFromUrl(String url) {
    if (url.contains("mysql")) {
      return "com.mysql.cj.jdbc.Driver";
    } else if (url.contains("postgresql") || url.contains("pgsql")) {
      return "org.postgresql.Driver";
    } else if (url.contains("mariadb")) {
      return "org.mariadb.jdbc.Driver";
    } else if (url.contains("hive") || url.contains("spark")) {
      return "org.apache.hive.jdbc.HiveDriver";
    } else {
      log.warn("Could not determine driver for URL: {}. Using generic driver.", url);
      return "java.sql.Driver";
    }
  }

  @Bean
  public ChatLanguageModel geminiChatModel() {
    log.info("Initializing Gemini Chat Model...");
    try {
      ChatLanguageModel model =
          GoogleAiGeminiChatModel.builder()
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

  @Bean(name = "openaiChatLanguageModel")
  public ChatLanguageModel openAiChatModel() {
    log.info("Initializing OpenAI Chat Model...");
    try {
      ChatLanguageModel model =
          OpenAiChatModel.builder()
              .apiKey(openaiApiKey)
              .modelName("GPT_4_O_MINI")
              .logRequests(true)
              .logResponses(true)
              .build();
      log.info("OpenAI Chat Model initialized successfully.");
      return model;
    } catch (Exception e) {
      log.error("Failed to initialize OpenAI Chat Model: {}", e.getMessage(), e);
      throw new ModelInitializationException("OpenAI Chat Model initialization failed", e);
    }
  }

  @Bean(name = "ollamaChatLanguageModel")
  public ChatLanguageModel ollamaChatModel() {
    log.info("Initializing Ollama Chat Model...");
    try {
      ChatLanguageModel model =
          OllamaChatModel.builder()
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
      ChatLanguageModel model =
          LocalAiChatModel.builder()
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
  public EmbeddingModel embeddingModel() {
    log.info("Initializing Embedding Model...");
    EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    return embeddingModel;
  }

  @Bean
  public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
    log.info("Initializing Embedding Store...");
    EmbeddingStore<TextSegment> embeddingStore =
        PgVectorEmbeddingStore.builder()
            .host(pgVectorHost)
            .port(pgVectorPort)
            .database(pgVectorDatabase)
            .user(pgVectorUser)
            .password(pgVectorPassword)
            .table(pgVectorTable)
            .dimension(embeddingModel.dimension())
            .build();
    return embeddingStore;
  }

  @Bean
  public ContentRetriever sqlDatabaseContentRetriever(
      DataSource dataSource,
      String[] schemaType,
      EmbeddingStore<TextSegment> embeddingStore,
      EmbeddingModel embeddingModel,
      ChatLanguageModel geminiChatModel,
      @Qualifier("ollamaChatLanguageModel") ChatLanguageModel ollamaChatModel,
      @Qualifier("openaiChatLanguageModel") ChatLanguageModel openaiChatModel) {

    Map<String, String> tables = fhirDbConfig != null ? fhirDbConfig.getTables() : new HashMap<>();

    List<Object[]> obs = new ArrayList<>();
    for (Map.Entry<String, String> entry : tables.entrySet()) {
      String tableName = entry.getKey();
      String[] columns = entry.getValue().split(",");

      List<Object[]> tableResults = selectColumnsFromTable(tableName, columns, dataSource);
      if (tableResults != null) {
        obs.addAll(tableResults);
      }
    }

    StringBuilder output = new StringBuilder();

    for (Object[] row : obs) {
      for (Object value : row) {
        if (value != null) {
          output.append(value).append("\n");
          log.debug("Metadata for embedding: {}", value);
        }
      }
    }

    if (output != null && !output.toString().isBlank()) {
      Document document = Document.document(output.toString());

      List<TextSegment> segments = split(document);

      List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
      embeddingStore.addAll(embeddings, segments);
    }
    return SqlDatabaseContentRetriever.builder()
        .dataSource(dataSource)
        .chatLanguageModel(openaiChatModel)
        .ollamaChatModel(ollamaChatModel)
        .tables(tables)
        .schemaType(schemaType)
        .build();
  }

  @Bean
  public Assistant assistant(
      ChatLanguageModel geminiChatModel, ContentRetriever sqlDatabaseContentRetriever) {
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

  public List<TextSegment> split(Document document) {
    ensureNotNull(document, "document");

    List<TextSegment> segments = new ArrayList<>();

    String[] parts = document.text().split("\n");
    for (int i = 0; i < parts.length; i++) {
      if (parts[i] != null && !parts[i].isEmpty()) {
        segments.add(createSegment(parts[i], document, i));
      }
    }
    return segments;
  }

  static TextSegment createSegment(String text, Document document, int index) {
    Metadata metadata = document.metadata().copy().put("index", String.valueOf(index));
    return TextSegment.from(text, metadata);
  }

  private static List<Object[]> selectColumnsFromTable(
      String tableName, String[] columns, DataSource dataSource) {
    List<Object[]> results = new ArrayList<>();

    // Ensure we have columns to select
    if (columns.length == 0) {
      throw new IllegalArgumentException("At least one column must be specified");
    }

    // Build the SQL query with only the specified columns
    StringBuilder queryBuilder = new StringBuilder("SELECT ");

    for (int i = 0; i < columns.length; i++) {
      queryBuilder.append(columns[i]);
      if (i < columns.length - 1) {
        queryBuilder.append(", ");
      }
    }

    queryBuilder.append(" FROM ").append(tableName);
    String query = queryBuilder.toString();

    // Execute the query
    // try (Connection conn = DriverManager.getConnection("jdbc:hive2://spark:10000");
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      // Process each row
      while (rs.next()) {
        // Create an array with the same length as the columns array
        Object[] rowValues = new Object[columns.length];

        // Fill the array with values in the same order as the columns array
        for (int i = 0; i < columns.length; i++) {
          rowValues[i] = rs.getObject(columns[i]);
        }

        results.add(rowValues);
      }
    } catch (SQLException e) {
      System.err.println("Error executing query: " + e.getMessage());
      e.printStackTrace();
    }

    return results;
  }
}

// Custom exception for model initialization errors
class ModelInitializationException extends RuntimeException {
  public ModelInitializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
