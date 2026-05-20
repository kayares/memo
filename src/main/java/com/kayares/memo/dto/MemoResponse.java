package com.kayares.memo.dto;

import com.kayares.memo.domain.Memo;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MemoResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;
    private final String username;

    public MemoResponse(Memo memo) {
        this.id = memo.getId();
        this.title = memo.getTitle();
        this.content = memo.getContent();
        this.createdAt = memo.getCreatedAt();
        this.username = memo.getUser().getUsername();
    }
}