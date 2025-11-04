package org.harry.ascholar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswerRequest {

        private String answer;
        private LocalDateTime timestamp;
        private boolean reviewed = false;

}
