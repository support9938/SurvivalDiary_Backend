package com.survivaldiary.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePostRequest(
        @NotBlank @Size(max = 30) String category,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        List<@Size(max = 100) String> hashtags,
        List<@Size(max = 5_000_000) String> imageUrls,
        @Size(max = 20) String imageAlignment
) {}
