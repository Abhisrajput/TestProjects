package com.zbank.modernization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Account Inquiry Request DTO
 * 
 * Maps from legacy CICS ZBANK3 program input fields:
 * - COMMAREA account number field (PIC X(8))
 * - Terminal/user identification
 * 
 * Traceability:
 * - WF-001: Account Inquiry workflow
 * - Legacy: CICS.COB_ZBANK3_.cbl COMMAREA structure
 * - BR-001: Account number validation (8 digits)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInquiryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Account number - maps from legacy PIC X(8) ACCT-NO field
     * Must be 8 digits as per legacy VSAM key structure
     */
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{8}$", message = "Account number must be exactly 8 digits")
    @JsonProperty("accountNumber")
    private String accountNumber;

    /**
     * User/Terminal ID initiating inquiry
     * Maps from CICS EIBTRMID (4-character terminal ID)
     */
    @Size(max = 16, message = "User ID must not exceed 16 characters")
    @JsonProperty("userId")
    private String userId;

    /**
     * Optional transaction reference for audit trail
     * Maps to legacy transaction tracking
     */
    @Size(max = 32, message = "Transaction reference must not exceed 32 characters")
    @JsonProperty("transactionRef")
    private String transactionRef;

    /**
     * Include transaction history flag
     * Legacy default: false (basic inquiry only)
     */
    @JsonProperty("includeHistory")
    @Builder.Default
    private Boolean includeHistory = false;

    /**
     * Number of recent transactions to retrieve if includeHistory=true
     * Default matches legacy display limit
     */
    @JsonProperty("historyLimit")
    @Builder.Default
    private Integer historyLimit = 10;

    /**
     * Converts legacy COBOL COMMAREA format to DTO
     * Handles padding/trimming from fixed-width mainframe fields
     * 
     * @param accountNumber Legacy 8-char account number (may be padded)
     * @param terminalId Legacy 4-char terminal ID
     * @return Populated AccountInquiryRequest
     */
    public static AccountInquiryRequest fromLegacyFormat(String accountNumber, String terminalId) {
        return AccountInquiryRequest.builder()
                .accountNumber(accountNumber != null ? accountNumber.trim() : null)
                .userId(terminalId != null ? terminalId.trim() : null)
                .includeHistory(false)
                .historyLimit(10)
                .build();
    }

    /**
     * Normalizes account number to match legacy format expectations
     * Ensures consistent formatting for VSAM key lookups during migration phase
     * 
     * @return Normalized 8-digit account number
     */
    public String getNormalizedAccountNumber() {
        if (accountNumber == null) {
            return null;
        }
        return String.format("%8s", accountNumber).replace(' ', '0');
    }

    /**
     * Validates request meets legacy business rules
     * Additional validation beyond annotations for complex rules
     * 
     * @return true if valid for legacy system interaction
     */
    public boolean isValidForLegacySystem() {
        return accountNumber != null 
                && accountNumber.matches("^[0-9]{8}$")
                && (historyLimit == null || historyLimit <= 100);
    }
}