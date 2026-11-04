
package com.example.veylora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(

        @NotBlank(message = "Contact name is required")
        @Size(max = 100)
        String name,

        @Size(max = 30)
        String phone,

        @Email(message = "Enter a valid email address")
        @Size(max = 150)
        String email,

        @Size(max = 150)
        String company,

        @Size(max = 1000)
        String notes
) {}
