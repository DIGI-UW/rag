package org.uwdigi.rag.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FhirDbConfig {
  private static final Logger log = LoggerFactory.getLogger(FhirDbConfig.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private Map<String, String> tables = new HashMap<>();

  public Map<String, String> getTables() {
    return tables;
  }

  public void setTables(Map<String, String> tables) {
    this.tables = tables;
  }

  @Value("${FHIR_DB_TABLES}")
  private String tablesJson;

  @PostConstruct
  public void initializeTables() {
    try {
      log.info("Raw FHIR_DB_TABLES value: [{}]", tablesJson);

      if (tablesJson == null) {
        log.error("FHIR_DB_TABLES is null");
        return;
      }

      String cleanJson = tablesJson.trim().replaceAll("^[\"']", "").replaceAll("[\"']$", "");

      log.info("Cleaned JSON string: [{}]", cleanJson);

      if (!cleanJson.startsWith("{") || !cleanJson.endsWith("}")) {
        log.error("Invalid JSON format. Must start with '{' and end with '}'");
        return;
      }

      tables = objectMapper.readValue(cleanJson, new TypeReference<Map<String, String>>() {});

      log.info("Initialized FHIR DB Tables from JSON:");
      tables.forEach((key, value) -> log.info("  Table: {} - Columns: {}", key, value));
    } catch (Exception e) {
      log.error("Failed to parse FHIR DB Tables configuration", e);

      tables = new HashMap<>();
    }
  }
}
