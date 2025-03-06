package org.uwdigi.rag.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.uwdigi.rag.service.AssistantService;

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
        String response = assistantService.processQuery(request.getQuery());
        return ResponseEntity.ok(new ChatResponse(response));
    }
    
    public static class ChatRequest {
        private String query;
        
        public ChatRequest() {}
        
        public ChatRequest(String query) {
            this.query = query;
        }
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
    }
    
    public static class ChatResponse {
        private String response;
        
        public ChatResponse() {}
        
        public ChatResponse(String response) {
            this.response = response;
        }
        
        public String getResponse() {
            return response;
        }
        
        public void setResponse(String response) {
            this.response = response;
        }
    }
}