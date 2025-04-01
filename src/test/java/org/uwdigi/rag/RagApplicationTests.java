package org.uwdigi.rag;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.uwdigi.rag.service.AssistantService;
import org.uwdigi.rag.shared.Assistant;

@SpringBootTest
class RagApplicationTests {

  @Autowired private Assistant assistant;

  @Autowired private AssistantService assistantService;

  @Test
  void contextLoads() {
    // Verify that the context loads and beans are properly initialized
    assertNotNull(assistant);
    assertNotNull(assistantService);
  }
}
