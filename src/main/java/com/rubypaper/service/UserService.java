package com.rubypaper.service;



import com.rubypaper.domain.BoardDTO;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.rubypaper.domain.User;
import com.rubypaper.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;

    // 회원가입
    public User registerUser(User user) throws Exception {
        // 중복 아이디 체크
        if (userRepository.existsByUserid(user.getUserid())) {
            throw new Exception("이미 존재하는 아이디입니다.");
        }
        return userRepository.save(user);
    }
    
    // 로그인 인증
    public User authenticateUser(String userid, String password) throws Exception {
        Optional<User> userOpt = userRepository.findByUserid(userid);
       
        if (userOpt.isEmpty()) {
            throw new Exception("존재하지 않는 아이디입니다.");
        }
        User user = userOpt.get();
        // 입력한 비밀번호와 DB에 저장된 비밀번호를 비교 (평문 비교)
        if (!userOpt.get().getPassword().equals(password)) {
            throw new Exception("비밀번호가 일치하지 않습니다.");
        }

       
        return user;
    }
    
    // 사용자 조회
    public Optional<User> findByUserid(String userid) {
        return userRepository.findByUserid(userid);
    }
    
    // 모든 사용자 조회
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // 사용자 삭제
    @Transactional
    public void deleteUser(@PathVariable String id) {userRepository.deleteUserByUserid(id);}

    @Transactional
    public List<BoardDTO> getUserBoards(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("유저 없음"));

        return user.getBoards().stream()
                .map(board -> {
                    String preview = abbreviate(board.getContent(), 100); // HTML 무시하고 텍스트만 자름

                    return new BoardDTO(
                            board.getId(),
                            board.getTitle(),
                            preview,
                            board.getCreatedDate()
                    );
                })
                .collect(Collectors.toList());
    }
    /// 마이페이지 폰트 적용되게 하는 함수 -> utext쓸 경우 div 생략되서 한 게시글에 모든 게시글이 포함됨.
    public String abbreviate(String content, int maxLength) {
        // HTML 태그 제거
        String plainText = content.replaceAll("<[^>]*>", ""); // <태그> 제거

        // 자르기
        if (plainText.length() > maxLength) {
            return plainText.substring(0, maxLength) + "...";
        } else {
            return plainText;
        }
    }

}
