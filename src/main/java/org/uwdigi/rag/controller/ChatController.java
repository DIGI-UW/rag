package org.uwdigi.rag.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uwdigi.rag.service.AssistantService;
import org.uwdigi.rag.shared.QueryResponse;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

  private final AssistantService assistantService;

  @Autowired
  public ChatController(AssistantService assistantService) {
    this.assistantService = assistantService;
  }

  @PostMapping
  public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    QueryResponse queryResponse =
        assistantService.processQuery(request.getQuery(), request.getModel());
    return ResponseEntity.ok(
        new ChatResponse(queryResponse.getResponse(), queryResponse.getSqlRun()));
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChatRequest {
    private String query;
    private String model;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChatResponse {
    private String response;
    private String logs;
  }
}
