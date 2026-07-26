package com.kayares.memo.controller;

import com.kayares.memo.domain.Memo;
import com.kayares.memo.dto.MemoCreateRequest;
import com.kayares.memo.dto.MemoResponse;
import com.kayares.memo.dto.MemoUpdateRequest;
import com.kayares.memo.service.MemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @PostMapping
    public ResponseEntity<MemoResponse> createMemo(
            @Valid @RequestBody MemoCreateRequest request,
            Authentication authentication) {

        Memo savedMemo = memoService.create(request.getTitle(),
                request.getContent(),
                authentication.getName());

        return ResponseEntity.ok(new MemoResponse(savedMemo));
    }

    @GetMapping
    public ResponseEntity<List<MemoResponse>> getMemos() {

        List<MemoResponse> memos = memoService
                .findAll()
                .stream()
                .map(MemoResponse::new)
                .toList();

        return ResponseEntity.ok(memos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemoResponse> getMemo(@PathVariable Long id) {

        Memo memo = memoService.findById(id);

        return ResponseEntity.ok(new MemoResponse(memo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoResponse> updateMemo(
            @PathVariable Long id,
            @Valid @RequestBody MemoUpdateRequest request,
            Authentication authentication) {

        Memo memo = memoService.update(id,
                request.getTitle(),
                request.getContent(),
                authentication.getName());

        return ResponseEntity.ok(new MemoResponse(memo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemo(
            @PathVariable Long id,
            Authentication authentication) {

        memoService.delete(id, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}