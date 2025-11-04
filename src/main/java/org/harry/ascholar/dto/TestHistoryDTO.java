package org.harry.ascholar.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TestHistoryDTO {
    private String testName;
    private BigDecimal score;
    private LocalDateTime completedAt;
    private String category;
}
