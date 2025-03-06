package org.uwdigi.rag.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uwdigi.rag.shared.Assistant;

@Service
public class AssistantService {

    private final Assistant assistant;
    
    @Autowired
    public AssistantService(Assistant assistant) {
        this.assistant = assistant;
    }
    
    public String processQuery(String query) {
        return assistant.answer(query);
    }
}