package org.harry.ascholar.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAnswersRequest {
    private Map<Long, String> answers; // questionId -> answer
    private Map<Long, LocalDateTime> answerTimestamps; // questionId -> timestamp
}
