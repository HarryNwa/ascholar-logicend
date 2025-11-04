package org.harry.ascholar.data.models;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Education {
    @NotBlank(message = "Institution name is required")
    private String institution;

    @NotBlank(message = "Degree is require")
    private String degree;

    @NotBlank(message = "Field of study is required")
    private String fieldOfStudy;

    private String startYear;
    private String endYear;
    private Boolean isCurrent;
    private String description;

}
