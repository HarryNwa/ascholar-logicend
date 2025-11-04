package org.harry.ascholar.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.harry.ascholar.data.models.TestAnswer;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TestAnswerResponse(
        Long id,
        Long attemptId,
        Long questionId,
        String answer,
        Boolean isCorrect,
        LocalDateTime answeredAt,
        Integer timeSpentOnQuestion,
        String questionType,
        Integer questionPoints,
        String normalizedAnswer,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TestAnswerResponse fromEntity(TestAnswer testAnswer) {
        if (testAnswer == null) {
            return null;
        }

        return TestAnswerResponse.builder()
                .id(testAnswer.getId())
                .attemptId(testAnswer.getAttempt() != null ? testAnswer.getAttempt().getId() : null)
                .questionId(testAnswer.getQuestionId())
                .answer(testAnswer.getAnswer())
                .isCorrect(testAnswer.getIsCorrect())
                .answeredAt(testAnswer.getAnsweredAt())
                .timeSpentOnQuestion(testAnswer.getTimeSpentOnQuestion())
                .build();
    }

    public static TestAnswerResponse fromEntityWithDetails(TestAnswer testAnswer, String questionType, Integer questionPoints) {
        TestAnswerResponse response = fromEntity(testAnswer);
        if (response != null) {
            return TestAnswerResponse.builder()
                    .id(response.id())
                    .attemptId(response.attemptId())
                    .questionId(response.questionId())
                    .answer(response.answer())
                    .isCorrect(response.isCorrect())
                    .answeredAt(response.answeredAt())
                    .timeSpentOnQuestion(response.timeSpentOnQuestion())
                    .questionType(questionType)
                    .questionPoints(questionPoints)
                    .build();
        }
        return null;
    }
}
