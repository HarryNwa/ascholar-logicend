package org.harry.ascholar.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TestAnalytics {
    private Long testId;
    private String testTitle;
    private long totalAttempts;
    private BigDecimal averageScore;
    private BigDecimal topScore;
    private double completionRate;

}
