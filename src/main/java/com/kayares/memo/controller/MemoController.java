package com.kayares.memo.controller;

import com.kayares.memo.domain.Memo;
import com.kayares.memo.domain.User;
import com.kayares.memo.dto.MemoCreateRequest;
import com.kayares.memo.dto.MemoResponse;
import com.kayares.memo.repository.MemoRepository;
import com.kayares.memo.repository.UserRepository;
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

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<MemoResponse> createMemo(
            @Valid @RequestBody MemoCreateRequest request,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Memo memo = new Memo(request.getTitle(), request.getContent(), user);
        Memo savedMemo = memoRepository.save(memo);
        return ResponseEntity.ok(new MemoResponse(savedMemo));
    }

    @GetMapping
    public ResponseEntity<List<MemoResponse>> getMemos() {
        List<MemoResponse> memos = memoRepository.findAll().stream()
                .map(MemoResponse::new)
                .toList();
        return ResponseEntity.ok(memos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemoResponse> getMemo(@PathVariable Long id) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 메모가 없습니다. id=" + id));
        return ResponseEntity.ok(new MemoResponse(memo));
    }
}