package org.harry.ascholar.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StudentTalentDTO {
    private Long studentId;
    private String studentName;
    private Long testId;
    private String testName;
    private BigDecimal score;
    private LocalDateTime completedAt;
    private String location;
    private String desiredProgram;
    private List<String> skills;
    private int universityInterests;
}
