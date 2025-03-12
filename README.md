# Spring Boot RAG SQL Assistant

This Spring Boot application implements an advanced Retrieval-Augmented Generation (RAG) system that connects language models with SQL database retrieval using LangChain4j.

## Features

- **Natural Language to SQL**: Ask questions in plain English and get database insights
- **Multiple LLM Support**: Works with Google Gemini, Ollama, or LocalAI
- **Interactive Interface**: Simple web UI for asking questions
- **Spring Boot Integration**: Leverages Spring's dependency injection and configuration

## Prerequisites

- Java 17 or higher
- Maven
- MariaDB database (or adjust configurations for your preferred database)
- API keys for language models (Google AI Gemini, etc.)

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/DIGI-UW/rag.git
cd rag
```

Sign in  to [Google Studio](https://aistudio.google.com) and get an API key for Gemini abd add it to the [env](.env) file

### Quick start with docker

#### Run App with Built docker images

```bash
docker compose up --build

```

#### Run App with published docker images
```bash
docker compose up

```

The application will be available at http://localhost:8080

### Configuration

Set up the following environment variables or modify `application.properties`:

```properties
# Database configuration
DB_URL=jdbc:mariadb://localhost:3306/your_database
DB_USER=your_username
DB_PASSWORD=your_password

# Language model API keys
GEMINI_API_KEY=your_gemini_api_key

# Ollama configuration (if using)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL_NAME=llama3

# LocalAI configuration (if using)
LOCAL_AI_BASE_URL=http://localhost:8080/v1
LOCAL_AI_MODEL_NAME=gpt-4
```

### Build and Run

```bash
# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will be available at http://localhost:8080

## Usage

1. Open your browser and navigate to http://localhost:8080
2. Type your database question in the chat interface
3. Receive answers based on your database content

Example questions:
- "How many patients do we have?"
- "What are the names of the patients?"
- "what was the most prescribed drug?"

## Architecture

- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Coordinates between components
- **Content Retriever**: Translates questions to SQL and retrieves data
- **LLM Integration**: Uses language models to generate SQL and format responses

## Security Warning

The `SqlDatabaseContentRetriever` is potentially dangerous and should be used with caution:

- **READ-ONLY ACCESS**: The database user should have very limited read-only permissions
- **DEVELOPMENT USE**: Not recommended for production without thorough security measures
- **SQL VALIDATION**: Basic validation is performed but cannot guarantee safety

## Extending the Application

### Adding More Language Models

Modify `AppConfig.java` to integrate additional language models:

```java
@Bean
public ChatLanguageModel newModel() {
    return NewModelProvider.builder()
            .apiKey("your-api-key")
            .modelName("model-name")
            .build();
}
```

### Customizing the UI

The frontend interface is located in `src/main/resources/static/index.html`. Modify this file to customize the user experience.

## Acknowledgments

- Built with [LangChain4j](https://github.com/langchain4j/langchain4j)
- Powered by Spring Boot