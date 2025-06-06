package org.uwdigi.rag.shared;

import static dev.langchain4j.internal.Utils.getOrDefault;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

  public static final String OPENAI_API_KEY = getOrDefault(System.getenv("OPENAI_API_KEY"), "demo");

  public static void startConversationWith(Assistant assistant) {
    Logger log = LoggerFactory.getLogger(Assistant.class);
    try (Scanner scanner = new Scanner(System.in)) {
      while (true) {
        log.info("==================================================");
        log.info("User: ");
        String userQuery = scanner.nextLine();
        log.info("==================================================");

        if ("exit".equalsIgnoreCase(userQuery)) {
          break;
        }

        String agentAnswer = assistant.answer(userQuery);
        log.info("==================================================");
        log.info("Assistant: " + agentAnswer);
      }
    }
  }

  public static PathMatcher glob(String glob) {
    return FileSystems.getDefault().getPathMatcher("glob:" + glob);
  }

  public static Path toPath(String relativePath) {
    try {
      URL fileUrl = Utils.class.getClassLoader().getResource(relativePath);
      if (fileUrl == null) {
        throw new IllegalArgumentException("Resource not found: " + relativePath);
      }
      return Paths.get(fileUrl.toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid URI syntax for resource: " + relativePath, e);
    }
  }
}
