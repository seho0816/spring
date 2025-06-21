package com.rubypaper.controller;


import com.rubypaper.domain.Board;
import com.rubypaper.domain.BoardDTO;
import jakarta.servlet.http.HttpSession;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rubypaper.domain.User;
import com.rubypaper.repository.UserRepository;
import com.rubypaper.service.UserService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Controller
public class LoginController {
    
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    // 메인 페이지 (로그인/회원가입 폼)
    @GetMapping("/login")
    public String index(Model model, HttpSession session) {
        // 이미 로그인된 경우 대시보드로 리다이렉트
        if (session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("user", new User());
        return "login";
    } ////// 지금 글 작성 시에 마이페이지에 가는 이유가 이거임
    // 로그인을 하고 글을 작성하지만 세션에 사용자 정보가 담겨있는지 아닌지는 모르겠고 아무튼 세션에서 읽어올 수가 없음
    // 그래서 코드가 로그인 폼으로 보내지만 재현이가 만든 로그인이 되있으면 대시보드로 보내는 이 코드가 작동하는거임 그래서 마이페이지로 가지는거
    
    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("user") User user,
                         //@Valid 로 검증한  결과를 저장
                        BindingResult bindingResult,
                        Model model,
                         // 리다이렉트(주소 이동) 후 데이터(메시지)를 전달할 목적
                        RedirectAttributes redirectAttributes ) {

        // 입력에 문제(@Notblank = 입력 안함) 생기면 실행
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error -> System.out.println(error));
            model.addAttribute("signupError", "입력 정보를 확인해주세요.");
            return "login";
        }
        
        try {
            userService.registerUser(user);
            // 리다이렉트 됐을 때 successMessage 이름으로 값 전달.
            redirectAttributes.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("signupError", e.getMessage());
            return "login";
        }
    }
    
    // 로그인 처리
    @PostMapping("/login")
    public String login(@RequestParam String userid,
                       @RequestParam String password, 
                       HttpSession session, 
                       Model model, 
                       RedirectAttributes redirectAttributes) {

        // try 안에 v0가 만든 코드 고치기 귀찮아서 그냥 2개 만듬. 정지 기간을 불러오는 변수


        try {
            User user = userService.authenticateUser(userid, password);
            session.setAttribute("user", user);
            
            LocalDate suspendDay= user.getSuspendedUntil();
            if (user.getSuspendedUntil() != null && user.getSuspendedUntil().isAfter(LocalDate.now())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");
                String suspendDate = user.getSuspendedUntil().format(formatter);
                model.addAttribute("suspendError", suspendDate + "까지 정지된 계정입니다.");
                model.addAttribute("user", new User());

                return "login"; // index.html 또는 로그인 페이지로 다시
            }

            //로그인 하면 자동으로 마이페이지 로 이동 -> 메인페이지로 수정 해야함
            return "redirect:/main";
        } catch (Exception e) {
            // 계정이 정지 당했을 때 오류 생성
            model.addAttribute("loginError", e.getMessage());
            model.addAttribute("user", new User());
            return "login";
        }
    }
    
    // 대시보드
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        List<BoardDTO> boardDTO = userService.getUserBoards(user.getId());
        if (user == null) {
        	System.out.println("세션에 사용자 정보가 없어요");
            return "redirect:/";
        }
        else {
        	System.out.println("로그인된 사용자 : "+ user.getUserid());
        }
        model.addAttribute("boards", boardDTO);
        model.addAttribute("user", user);
        model.addAttribute("allUsers", userService.findAllUsers());
        return "dashboard";
    }
    
    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "로그아웃되었습니다.");
        return "redirect:/";
    }

    // 정지 기간 설정
        @PostMapping("/users/lock/{id}")
        public String suspendUser(@PathVariable String id, @RequestParam int days) {
            Optional<User> user= userRepository.findByUserid(id);
            System.out.println(days);

            User user2= user.get();

            LocalDate suspendedUntil = LocalDate.now().plusDays(days);
            System.out.println(suspendedUntil);
            user2.setSuspendedUntil(suspendedUntil);
            userRepository.save(user2);

            return "redirect:/dashboard";
        }

        // 정지 해제
    @PostMapping("/users/unlock/{id}")
    public String unsuspendUser(@PathVariable String id) {
        Optional<User> user= userRepository.findByUserid(id);
        User user2= user.get();
        user2.setSuspendedUntil(null);
        userRepository.save(user2);

        return  "redirect:/dashboard";
    }
    // 유저 삭제
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable String id) {
        userService.deleteUser(id);

        return  "redirect:/dashboard";
    }

}
