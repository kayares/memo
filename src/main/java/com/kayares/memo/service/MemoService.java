package com.kayares.memo.service;

import com.kayares.memo.domain.Memo;
import com.kayares.memo.domain.User;
import com.kayares.memo.exception.MemoNotFoundException;
import com.kayares.memo.exception.UnauthorizedException;
import com.kayares.memo.exception.UserNotFoundException;
import com.kayares.memo.repository.MemoRepository;
import com.kayares.memo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    @Transactional
    public Memo create(String title, String content, String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

        Memo memo = new Memo(title, content, user);

        return memoRepository.save(memo);
    }

    public List<Memo> findAll() {

        return memoRepository.findAll();
    }

    public Memo findById(Long id) {

        return memoRepository.findById(id)
                .orElseThrow(() -> new MemoNotFoundException(id));
    }

    @Transactional
    public Memo update(Long id, String title, String content, String username) {

        Memo memo = findById(id);

        if (!memo.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("본인의 메모만 수정할 수 있습니다");
        }

        memo.update(title, content);

        return memo;
    }

    @Transactional
    public void delete(Long id, String username) {

        Memo memo = findById(id);

        if (!memo.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("본인의 메모만 삭제할 수 있습니다");
        }

        memoRepository.delete(memo);
    }
}
