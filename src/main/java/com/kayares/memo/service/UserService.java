package com.kayares.memo.service;

import com.kayares.memo.config.JwtTokenProvider;
import com.kayares.memo.domain.User;
import com.kayares.memo.exception.DuplicateUsernameException;
import com.kayares.memo.exception.InvalidCredentialsException;
import com.kayares.memo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public User signup(String username, String password) {

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException("이미 사용 중인 아이디입니다");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                .username(username)
                .password(encodedPassword)
                .build();

        return userRepository.save(user);
    }

    public String login(String username, String password) {

        String message = "아이디 또는 비밀번호가 올바르지 않습니다";

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException(message));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException(message);
        }

        return jwtTokenProvider.createToken(username);
    }
}
