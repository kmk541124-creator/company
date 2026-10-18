package me.kimmukwan.company.login.service;

import lombok.RequiredArgsConstructor;
import me.kimmukwan.company.login.domain.User;
import me.kimmukwan.company.login.dto.AddUserRequest;
import me.kimmukwan.company.login.dto.UserInfoResponse;
import me.kimmukwan.company.login.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Long save(AddUserRequest dto){
        // 이미 있는 아이디면 가입을 막음 (같은 아이디가 여러 개 생기면 로그인이 꼬임)
        if(userRepository.existsByUsername(dto.getUsername())){
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        return userRepository.save(User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .build()).getId();
    }

    // 내 정보 조회 — LOB는 고용할 때마다 바뀌므로 세션이 아닌 DB에서 최신 값을 읽음
    public UserInfoResponse getMyInfo(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        return UserInfoResponse.from(user);
    }
}
