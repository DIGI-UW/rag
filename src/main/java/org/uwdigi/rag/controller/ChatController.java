package org.uwdigi.rag.controller;

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

  public static class ChatRequest {
    private String query;
    private String model;

    public ChatRequest() {}

    public ChatRequest(String query, String model) {
      this.query = query;
      this.model = model;
    }

    public String getQuery() {
      return query;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }
  }

  public static class ChatResponse {
    private String response;
    private String logs;

    public ChatResponse() {}

    public ChatResponse(String response, String logs) {
      this.response = response;
      this.logs = logs;
    }

    public String getResponse() {
      return response;
    }

    public void setResponse(String response) {
      this.response = response;
    }

    public String getLogs() {
      return logs;
    }

    public void setLogs(String logs) {
      this.logs = logs;
    }
  }
}
