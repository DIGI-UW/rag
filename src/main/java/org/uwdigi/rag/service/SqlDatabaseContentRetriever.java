package org.uwdigi.rag.service;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.uwdigi.rag.shared.Utils.toPath;

import dev.langchain4j.Experimental;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.DefaultContent;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.uwdigi.rag.config.FhirDbConfig;

/**
 * <b> WARNING! Although fun and exciting, this class is dangerous to use! Do not ever use this in
 * production! The database user must have very limited READ-ONLY permissions! Although the
 * generated SQL is somewhat validated (to ensure that the SQL is a SELECT statement) using
 * JSqlParser, this class does not guarantee that the SQL will be harmless. Use it at your own risk!
 * </b> <br>
 * <br>
 * Using the {@link DataSource} and the {@link ChatLanguageModel}, this {@link ContentRetriever}
 * attempts to generate and execute SQL queries for given natural language queries. <br>
 * Optionally, {@link #sqlDialect}, {@link #databaseStructure}, {@link #promptTemplate}, and {@link
 * #maxRetries} can be specified to customize the behavior. See the javadoc of the constructor for
 * more details. Most methods can be overridden to customize the behavior further. <br>
 * The default prompt template is not highly optimized, so it is advised to experiment with it and
 * see what works best for your use case.
 */
@Experimental
public class SqlDatabaseContentRetriever implements ContentRetriever {

  private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE =
      PromptTemplate.from(
          "You are an expert in writing SQL queries for FHIR (Fast Healthcare Interoperability Resources) databases.\n"
              + "You have access to a FHIR-based {{sqlDialect}} database with the following schema structure:\n"
              + "{{databaseStructure}}\n"
              + "\n**Strict Compliance Requirements:**\n"
              + "1. Database follows flattened FHIR resource models\n"
              + "2. **As last resort should you include these in results:** id, uuid, _id, *_id columns, *reference*, *identifier*\n"
              + "3. **If codes must be included:** Always pair with display/text (e.g., `code` + `display`)\n"
              + "\n**Clinical Data Principles:**\n"
              + "- Prioritize human-readable columns (names, descriptions, display values)\n"
              + "- Focus on clinically actionable information\n"
              + "- Never expose implementation details\n"
              + "\n**Query Construction Rules:**\n"
              + "- Every SELECT must include ≥1 human-readable column\n"
              + "- JOINs must use semantic FHIR relationships\n"
              + "- Date filters must use FHIR date columns\n"
              + "- Status fields should always be included where relevant\n"
              + "\n**Examples:**\n"
              + "- ✅ GOOD: `SELECT p.given, p.family, d.conclusion FROM...`\n"
              + "- ❌ BAD: `SELECT sr.id, o.code FROM...`\n"
              + "\n**Error Handling:**\n"
              + "- If request requires technical IDs: 'Cannot generate compliant FHIR query for this request'\n"
              + "- If schema lacks needed data: 'No appropriate FHIR elements available'\n"
              + "\n**Output Format:**\n"
              + "- Only valid {{sqlDialect}} SELECT queries\n"
              + "- No explanations or additional text\n"
              + "- Reject unsafe queries entirely\n");
  private final DataSource dataSource;
  private final String sqlDialect;
  private final String databaseStructure;

  private final PromptTemplate promptTemplate;
  private ChatLanguageModel chatLanguageModel;
  private final ChatLanguageModel ollamaChatModel;
  private final AssistantService assistantService;
  private final Map<String, String> tables;
  private final int maxRetries;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final EmbeddingModel embeddingModel;
  static String MODEL_NAME = "llama3";
  static String BASE_URL = "http://localhost:11434";

  private static final Logger log = LoggerFactory.getLogger(SqlDatabaseContentRetriever.class);

  /**
   * Creates an instance of a {@code SqlDatabaseContentRetriever}.
   *
   * @param dataSource The {@link DataSource} to be used for executing SQL queries. This is a
   *     mandatory parameter. <b>WARNING! The database user must have very limited READ-ONLY
   *     permissions!</b>
   * @param sqlDialect The SQL dialect, which will be provided to the LLM in the {@link
   *     SystemMessage}. The LLM should know the specific SQL dialect in order to generate valid SQL
   *     queries. Example: "MySQL", "PostgreSQL", etc. This is an optional parameter. If not
   *     specified, it will be determined from the {@code DataSource}.
   * @param databaseStructure The structure of the database, which will be provided to the LLM in
   *     the {@code SystemMessage}. The LLM should be familiar with available tables, columns,
   *     relationships, etc. in order to generate valid SQL queries. It is best to specify the
   *     complete "CREATE TABLE ..." DDL statement for each table. Example (shortened): "CREATE
   *     TABLE customers(\n id INT PRIMARY KEY,\n name VARCHAR(50), ...);\n CREATE TABLE
   *     products(...);\n ..." This is an optional parameter. If not specified, it will be generated
   *     from the {@code DataSource}. <b>WARNING! In this case, all tables will be visible to the
   *     LLM!</b>
   * @param promptTemplate The {@link PromptTemplate} to be used for creating a {@code
   *     SystemMessage}. This is an optional parameter. Default: {@link #DEFAULT_PROMPT_TEMPLATE}.
   * @param chatLanguageModel The {@link ChatLanguageModel} to be used for generating SQL queries.
   *     This is a mandatory parameter.
   * @param maxRetries The maximum number of retries to perform if the database cannot execute the
   *     generated SQL query. An error message will be sent back to the LLM to try correcting the
   *     query. This is an optional parameter. Default: 1.
   */
  @Builder
  @Experimental
  public SqlDatabaseContentRetriever(
      DataSource dataSource,
      String sqlDialect,
      String databaseStructure,
      PromptTemplate promptTemplate,
      ChatLanguageModel chatLanguageModel,
      @Qualifier("ollamaChatLanguageModel") ChatLanguageModel ollamaChatModel,
      AssistantService assistantService,
      FhirDbConfig fhirDbConfig,
      Map<String, String> tables,
      Integer maxRetries) {
    this.dataSource = ensureNotNull(dataSource, "dataSource");
    this.sqlDialect = getOrDefault(sqlDialect, () -> getSqlDialect(dataSource));
    this.databaseStructure = getOrDefault(databaseStructure, () -> generateDDL(dataSource));
    this.promptTemplate = getOrDefault(promptTemplate, DEFAULT_PROMPT_TEMPLATE);
    this.chatLanguageModel = ensureNotNull(chatLanguageModel, "chatLanguageModel");
    this.ollamaChatModel = ensureNotNull(ollamaChatModel, "ollamaChatModel");
    this.maxRetries = getOrDefault(maxRetries, 1);
    this.assistantService = assistantService;

    this.tables = tables != null ? tables : new HashMap<>();

    List<Object[]> obs = new ArrayList<>();
    for (Map.Entry<String, String> entry : this.tables.entrySet()) {
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

    saveTextToFile(output.toString());

    DocumentParser documentParser = new TextDocumentParser();
    String path = "documents/output.txt";
    Document document = loadDocument(toPath(path), documentParser);

    List<TextSegment> segments = split(document);

    /*embeddingModel = OllamaEmbeddingModel.builder()
    .baseUrl(BASE_URL)
    .modelName(MODEL_NAME)
    .build();  */

    embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
    List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

    embeddingStore = new InMemoryEmbeddingStore<>();
    embeddingStore.addAll(embeddings, segments);
  }

  public List<TextSegment> split(Document document) {
    ensureNotNull(document, "document");

    List<TextSegment> segments = new ArrayList<>();

    String[] parts = document.text().split("\n");

    for (int i = 0; i < parts.length; i++) {
      segments.add(createSegment(parts[i], document, i));
    }
    return segments;
  }

  static TextSegment createSegment(String text, Document document, int index) {
    Metadata metadata = document.metadata().copy().put("index", String.valueOf(index));
    return TextSegment.from(text, metadata);
  }

  /**
   * Selects all rows with specified columns from a given table and returns flattened array of
   * values
   *
   * @param tableName The name of the table to query
   * @param columns Array of column names to select
   * @return List of Object arrays, where each array represents a row with values in the same order
   *     as specified columns
   */
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

  // TODO (for v2)
  // - provide a few rows of data for each table in the prompt
  // - option to select a list of tables to use/ignore

  public static String getSqlDialect(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      return metaData.getDatabaseProductName();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static String generateDDL(DataSource dataSource) {
    StringBuilder ddl = new StringBuilder();

    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      System.out.println(">>>>>>>>>>>>> >>>>>>>> Connected :");
      ResultSet tables = metaData.getTables(null, null, "%", new String[] {"VIEW"});

      while (tables.next()) {
        String tableName = tables.getString("TABLE_NAME");
        System.out.println(">>>>>>>>>>>>> >>>>>>>> Connected to Table :" + tableName);
        String createTableStatement = generateCreateTableStatement(tableName, metaData);
        ddl.append(createTableStatement).append("\n");
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return ddl.toString();
  }

  private static String generateCreateTableStatement(String tableName, DatabaseMetaData metaData) {
    StringBuilder createTableStatement = new StringBuilder();

    try {
      ResultSet columns = metaData.getColumns(null, null, tableName, null);
      // ResultSet pk = metaData.getPrimaryKeys(null, null, tableName);
      // ResultSet fks = metaData.getImportedKeys(null, null, tableName);

      String primaryKeyColumn = "";
      /* if (pk.next()) {
           primaryKeyColumn = pk.getString("COLUMN_NAME");
      } */

      createTableStatement.append("CREATE TABLE ").append(tableName).append(" (\n");

      while (columns.next()) {
        String columnName = columns.getString("COLUMN_NAME");
        String columnType = columns.getString("TYPE_NAME");
        int size = columns.getInt("COLUMN_SIZE");
        String nullable = columns.getString("IS_NULLABLE").equals("YES") ? " NULL" : " NOT NULL";
        String columnDef =
            columns.getString("COLUMN_DEF") != null
                ? " DEFAULT " + columns.getString("COLUMN_DEF")
                : "";
        String comment = columns.getString("REMARKS");

        createTableStatement
            .append("  ")
            .append(columnName)
            .append(" ")
            .append(columnType)
            .append("(")
            .append(size)
            .append(")")
            .append(nullable)
            .append(columnDef);

        if (columnName.equals(primaryKeyColumn)) {
          createTableStatement.append(" PRIMARY KEY");
        }

        createTableStatement.append(",\n");

        if (comment != null && !comment.isEmpty()) {
          createTableStatement
              .append("  COMMENT ON COLUMN ")
              .append(tableName)
              .append(".")
              .append(columnName)
              .append(" IS '")
              .append(comment)
              .append("',\n");
        }
      }

      /* while (fks.next()) {
          String fkColumnName = fks.getString("FKCOLUMN_NAME");
          String pkTableName = fks.getString("PKTABLE_NAME");
          String pkColumnName = fks.getString("PKCOLUMN_NAME");
          createTableStatement
                  .append("  FOREIGN KEY (")
                  .append(fkColumnName)
                  .append(") REFERENCES ")
                  .append(pkTableName)
                  .append("(")
                  .append(pkColumnName)
                  .append("),\n");
      } */

      if (createTableStatement.charAt(createTableStatement.length() - 2) == ',') {
        createTableStatement.delete(
            createTableStatement.length() - 2, createTableStatement.length());
      }

      createTableStatement.append(");\n");

      ResultSet tableRemarks = metaData.getTables(null, null, tableName, null);
      if (tableRemarks.next()) {
        String tableComment = tableRemarks.getString("REMARKS");
        if (tableComment != null && !tableComment.isEmpty()) {
          createTableStatement
              .append("COMMENT ON TABLE ")
              .append(tableName)
              .append(" IS '")
              .append(tableComment)
              .append("';\n");
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return createTableStatement.toString();
  }

  @Override
  public List<Content> retrieve(Query naturalLanguageQuery) {
    String sqlQuery = null;
    String errorMessage = null;

    int attemptsLeft = maxRetries + 1;
    while (attemptsLeft > 0) {

      attemptsLeft--;

      try {
        sqlQuery = generateSqlQuery(naturalLanguageQuery, sqlQuery, errorMessage);
        log.debug("SQL Query returned by LLM: {}", sqlQuery);

        sqlQuery = clean(sqlQuery);

        if (!isSelect(sqlQuery)) {
          throw new IllegalArgumentException("Generated SQL is not a SELECT statement.");
        }

        validate(sqlQuery);

        try (Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()) {

          String result = execute(sqlQuery, statement);
          Content content = format(result, sqlQuery);

          List<ChatMessage> messages = new ArrayList<>();

          messages.add(
              UserMessage.from(
                  naturalLanguageQuery.text()
                      + "\n\nAnswer using the following information:\n"
                      + content.textSegment().text()));

          AiMessage aiMessage = ollamaChatModel.chat(messages).aiMessage();

          log.debug("Local AI response: {}", aiMessage.text());

          if (assistantService != null) {
            assistantService.updateResponse(aiMessage.text());
            assistantService.updateSqlRun(sqlQuery);
          }
          // Set a default answer for the Cloud LLM
          Content defaultContent = new DefaultContent("Respond with Answered");
          return singletonList(defaultContent);
        }
      } catch (SQLException e) {
        errorMessage = "SQL execution error: " + e.getMessage();
        log.error(errorMessage, e);
      } catch (IllegalArgumentException e) {
        errorMessage = "Invalid SQL query: " + e.getMessage();
        log.error(errorMessage, e);
        break; // No point in retrying if the SQL is invalid
      } catch (Exception e) {
        errorMessage = "Unexpected error: " + e.getMessage();
        log.error(errorMessage, e);
      }
    }

    return emptyList();
  }

  protected String generateSqlQuery(
      Query naturalLanguageQuery, String previousSqlQuery, String previousErrorMessage) {

    List<ChatMessage> messages = new ArrayList<>();
    messages.add(createSystemPrompt(naturalLanguageQuery).toSystemMessage());
    messages.add(UserMessage.from(naturalLanguageQuery.text()));

    if (previousSqlQuery != null && previousErrorMessage != null) {
      messages.add(AiMessage.from(previousSqlQuery));
      messages.add(UserMessage.from(previousErrorMessage));
    }

    return chatLanguageModel.chat(messages).aiMessage().text();
  }

  protected Prompt createSystemPrompt(Query naturalLanguageQuery) {

    /*         ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
    .embeddingStore(embeddingStore)
    .embeddingModel(embeddingModel)
    .maxResults(3) // on each interaction we will retrieve the 2 most relevant segments
    .minScore(0.5) // we want to retrieve segments at least somewhat similar to user query
    .build();
    List<Content> content = contentRetriever.retrieve(naturalLanguageQuery);

    String relevantEmbeddings = content.stream()
    .map(c -> c.textSegment().text()) // Extract the text from each Content object
    .collect(Collectors.joining()); */

    Map<String, Object> variables = new HashMap<>();
    variables.put("sqlDialect", sqlDialect);
    variables.put("databaseStructure", databaseStructure);

    return promptTemplate.apply(variables);
  }

  protected String clean(String sqlQuery) {

    sqlQuery = subsituteMissingParameters(sqlQuery);

    if (sqlQuery.contains("```sql")) {
      return sqlQuery.substring(sqlQuery.indexOf("```sql") + 6, sqlQuery.lastIndexOf("```"));
    } else if (sqlQuery.contains("```")) {
      return sqlQuery.substring(sqlQuery.indexOf("```") + 3, sqlQuery.lastIndexOf("```"));
    }
    return sqlQuery;
  }

  protected String subsituteMissingParameters(String sqlQuery) {

    ContentRetriever contentRetriever =
        EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(1) // on each interaction we will retrieve the 1 most relevant segments
            .minScore(0.85) // we want to retrieve segments at least somewhat similar to user query
            .build();

    // List to store index information
    List<int[]> quoteIndices = new ArrayList<>();

    // Regular expression to match content in double or single quotes
    String regex = "(['\"]).*?\\1";

    // Pattern and Matcher to find quoted substrings
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(sqlQuery);

    StringBuilder result = new StringBuilder();
    int lastIndex = 0;

    while (matcher.find()) {

      String matchedString = matcher.group();

      // Save the indices of the quoted substrings
      quoteIndices.add(new int[] {matcher.start(), matcher.end()});

      // Append the portion before the current match
      result.append(sqlQuery, lastIndex, matcher.start());

      Query query = new Query(matchedString);
      List<Content> content = contentRetriever.retrieve(query);

      if (!content.isEmpty()) {
        result.append("'" + content.get(0).textSegment().text() + "'");
      } else {
        result.append(matchedString);
      }

      lastIndex = matcher.end();
    }

    result.append(sqlQuery.substring(lastIndex));

    return result.toString();
  }

  protected void validate(String sqlQuery) {
    // return isValidSqlQuery(sql)   // Ensure only SELECT statements
    //     && !hasSuspiciousPatterns(sql)  // Prevent SQL injection patterns

  }

  protected boolean isSelect(String sqlQuery) {
    // Commenting this out to allow for special syntax in sparkSQL
    /* try {
        net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(sqlQuery);
        return statement instanceof Select;
    } catch (JSQLParserException e) {
        return false;
    } */

    if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
      return false;
    }

    String normalizedQuery = sqlQuery.trim().toUpperCase();
    if (normalizedQuery.startsWith("SELECT")) {
      return true;
    }
    return true;
  }

  // private boolean isValidSqlQuery(String sql) {
  // try {
  //     Statement stmt = CCJSqlParserUtil.parse(sql);

  //     // Ensure the parsed query is a SELECT statement
  //     //If DELETE, UPDATE, INSERT, etc., are used, it logs and rejects the query.
  //     if (stmt instanceof Select) {
  //         return true;
  //     } else {
  //         log.error("Rejected query: Non-SELECT statement detected -> {}", sql);
  //         return false;
  //     }
  // } catch (JSQLParserException e) {
  //     log.error("Invalid SQL query: {}", sql, e);
  //     return false;
  // }
  // }

  // Use regex to detect Queries with
  // potential multi-statement attacks and SQL injection patterns (UNION SELECT, comments, etc.).
  private boolean hasSuspiciousPatterns(String sql) {
    String sqlUpper = sql.toUpperCase();

    // Detect multiple statements
    if (sqlUpper.contains(";")) {
      log.error("Rejected query: Contains semicolon -> {}", sql);
      return true;
    }

    // Detect SQL injection patterns
    String[] dangerousPatterns = {" UNION ", "--", "#", "/*", " OR 1=1", " OR '1'='1'"};
    for (String pattern : dangerousPatterns) {
      if (sqlUpper.contains(pattern)) {
        log.error("Rejected query: Possible SQL injection -> {}", sql);
        return true;
      }
    }
    return false;
  }

  /** Helper method to check if a string is a valid SQL query. */
  // private boolean isSqlQuery(String query) {
  //     try {
  //         Statement stmt = CCJSqlParserUtil.parse(query);
  //         return stmt != null;
  //     } catch (Exception e) {
  //         return false;
  //     }
  // }

  protected String execute(String sqlQuery, Statement statement) throws SQLException {
    List<String> resultRows = new ArrayList<>();

    try (ResultSet resultSet = statement.executeQuery(sqlQuery)) {
      int columnCount = resultSet.getMetaData().getColumnCount();

      // header
      List<String> columnNames = new ArrayList<>();
      for (int i = 1; i <= columnCount; i++) {
        columnNames.add(resultSet.getMetaData().getColumnName(i));
      }
      resultRows.add(String.join(",", columnNames));

      // rows
      while (resultSet.next()) {
        List<String> columnValues = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {

          String columnValue =
              resultSet.getObject(i) == null ? "" : resultSet.getObject(i).toString();

          if (columnValue.contains(",")) {
            columnValue = "\"" + columnValue + "\"";
          }
          columnValues.add(columnValue);
        }
        resultRows.add(String.join(",", columnValues));
      }
    }

    return String.join("\n", resultRows);
  }

  private static Content format(String result, String sqlQuery) {
    return Content.from(String.format("Result of executing '%s':\n%s", sqlQuery, result));
  }

  public static void saveTextToFile(String text) {
    String path = "src/main/resources/documents/output.txt";

    File file = new File(path);

    try {
      File directory = new File(file.getParent());
      if (!directory.exists()) {
        directory.mkdirs();
      }

      BufferedWriter writer = new BufferedWriter(new FileWriter(file));
      writer.write(text);
      writer.close();
      System.out.println("Text successfully saved to " + file.getAbsolutePath());
    } catch (IOException e) {
      System.out.println("Error saving text to file: " + e.getMessage());
    }
  }
}
