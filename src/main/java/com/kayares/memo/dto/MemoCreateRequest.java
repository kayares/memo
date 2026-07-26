package com.kayares.memo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class MemoCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 1000, message = "내용은 1000자 이내여야 합니다")
    private String content;
}