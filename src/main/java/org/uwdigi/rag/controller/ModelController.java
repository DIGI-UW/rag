package org.uwdigi.rag.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uwdigi.rag.config.ModelConfig;
import org.uwdigi.rag.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST controller for managing model switching.
 */
@RestController
@RequestMapping("/api/model")
public class ModelController {

    private static final Logger log = LoggerFactory.getLogger(ModelController.class);
    private final ModelConfig modelConfig;

    public ModelController(ModelConfig modelConfig) {
        this.modelConfig = modelConfig;
    }

    /**
     * Get the currently active model.
     * 
     * @return The currently active model
     */
    @GetMapping("/current")
    public ResponseEntity<ModelResponse> getCurrentModel() {
        log.info("Getting current model type: {}", modelConfig.getActiveModel());
        return ResponseEntity.ok(new ModelResponse(modelConfig.getActiveModel()));
    }

    /**
     * Switch to a different model.
     * 
     * @param modelType The model type to switch to
     * @return The new active model
     */
    @PostMapping("/switch/{modelType}")
    public ResponseEntity<ModelResponse> switchModel(@PathVariable ModelType modelType) {
        log.info("Switching model to: {}", modelType);
        modelConfig.setActiveModel(modelType);
        return ResponseEntity.ok(new ModelResponse(modelConfig.getActiveModel()));
    }

    /**
     * Response class for model operations.
     */
    public static class ModelResponse {
        private ModelType modelType;
        private String message;

        public ModelResponse() {
        }

        public ModelResponse(ModelType modelType) {
            this.modelType = modelType;
            this.message = "Active model is: " + modelType;
        }

        public ModelType getModelType() {
            return modelType;
        }

        public void setModelType(ModelType modelType) {
            this.modelType = modelType;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}