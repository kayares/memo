package com.kayares.memo.exception;

public class MemoNotFoundException extends RuntimeException {

    public MemoNotFoundException(Long id) {
        super("메모를 찾을 수 없습니다: " + id);
    }
}