package org.harry.ascholar.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TalentSearchCriteria {
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private String testCategory;
    private String location;
    private String desiredProgram;
    private List<String> skills;
    private LocalDateTime completedAfter;

}
