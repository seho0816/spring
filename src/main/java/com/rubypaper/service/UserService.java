package com.rubypaper.service;



import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.rubypaper.domain.User;
import com.rubypaper.repository.UserRepository;

import java.util.List;
import java.util.Optional;

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
}
