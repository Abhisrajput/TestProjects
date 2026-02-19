package com.zbank.modernization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RFC 7807 Problem Details for HTTP APIs compliant error response.
 * Provides structured validation error information with traceability.
 * Maps to Stage 1 NFR-002: Error Handling Requirements
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured error response following RFC 7807")
public class ValidationErrorResponse {
    
    @Schema(description = "URI reference identifying the problem type", example = "https://api.zbank.com/problems/validation-error")
    private String type;
    
    @Schema(description = "Short, human-readable summary", example = "Validation Failed")
    private String title;
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Human-readable explanation", example = "Request validation failed")
    private String detail;
    
    @Schema(description = "URI reference identifying the specific occurrence")
    private String instance;
    
    @Schema(description = "Unique correlation ID for request tracing")
    private String correlationId;
    
    @Schema(description = "Timestamp of error occurrence")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
    
    @Schema(description = "List of specific field validation errors")
    private List<FieldError> errors;
    
    public ValidationErrorResponse() {
        this.correlationId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.errors = new ArrayList<>();
    }
    
    public ValidationErrorResponse(String type, String title, int status, String detail) {
        this();
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
    }
    
    public void addFieldError(String field, String message, Object rejectedValue) {
        errors.add(new FieldError(field, message, rejectedValue));
    }
    
    @Schema(description = "Individual field validation error")
    public static class FieldError {
        @Schema(description = "Field name that failed validation", example = "accountNumber")
        private String field;
        
        @Schema(description = "Validation error message", example = "Account number must be 10 digits")
        private String message;
        
        @Schema(description = "Value that was rejected", example = "123")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object rejectedValue;
        
        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        // Getters and setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getRejectedValue() { return rejectedValue; }
        public void setRejectedValue(Object rejectedValue) { this.rejectedValue = rejectedValue; }
    }
    
    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getInstance() { return instance; }
    public void setInstance(String instance) { this.instance = instance; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public List<FieldError> getErrors() { return errors; }
    public void setErrors(List<FieldError> errors) { this.errors = errors; }
}