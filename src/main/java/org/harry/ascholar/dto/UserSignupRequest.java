package org.harry.ascholar.dto;

import lombok.Data;
import org.harry.ascholar.data.enums.UserRole;

@Data
public class UserSignupRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private UserRole role;
}
