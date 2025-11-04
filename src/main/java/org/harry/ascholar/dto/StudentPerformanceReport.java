package org.harry.ascholar.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class StudentPerformanceReport {
    private Long userId;
    private int totalTestsTaken;
    private BigDecimal averageScore;
    private BigDecimal bestScore;
    private List<TestHistoryDTO> testHistory;
    private Map<String, BigDecimal> skillBreakdown;

}
