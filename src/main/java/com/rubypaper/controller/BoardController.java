package com.rubypaper.controller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Certificate;
import com.rubypaper.domain.User;
import com.rubypaper.service.BoardService;
import com.rubypaper.service.CertificateService;
import com.rubypaper.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class BoardController {

	 @Autowired
	    private BoardService boardService;
	    
	    @Autowired
	    private UserService userService;
	    
	    @Autowired
	    private CertificateService certificateService; // 자격증 서비스 추가

	    // 게시판 목록 조회 (게시판 페이지)
	    @GetMapping("/board/list")
	    public String listBoards(@RequestParam("category") String category,
	                             @RequestParam("subcategory") String subcategory,
	                             @RequestParam("certificateName") String certificateName,
	                             Model model) {
	         // 카테고리와 서브카테고리로 게시글 목록을 가져옵니다.
	    	List<String> subcategories = boardService.getSubcategoriesForCategory(category);
	    	 model.addAttribute("subcategories", subcategories); 
	    	 
	         List<Board> boards = boardService.getBoardsByCategoryAndSubcategory(category, subcategory);
	         model.addAttribute("boards", boards);

	         List<Certificate> certificates = boardService.getCertificatesForCategoryAndSubcategory(category, subcategory);
	         model.addAttribute("certificates", certificates);
	         // 카테고리, 서브카테고리, 자격증 정보도 모델에 추가
	         model.addAttribute("selectedCategory", category);
	         model.addAttribute("selectedSubcategory", subcategory);
	         model.addAttribute("selectedCertificate", certificateName);
	         
	         // 자격증 목록 추가
	         

	         return "board/list"; // 게시판 목록 페이지
	     }

    // 게시글 상세 페이지
    @GetMapping("/board/{id}")
    public String boardDetail(@PathVariable("id") Long id, Model model) {
        // 게시글을 ID로 찾기
        Board board = boardService.getBoardById(id).get();
        model.addAttribute("board", board);
        return "board/detail"; // 게시글 상세 페이지
    }
        
        @PostMapping("/board/save")
        public String saveBoard(@ModelAttribute Board board, 
                                @RequestParam("image") MultipartFile image, 
                                HttpSession session) throws Exception {

            // 로그인된 사용자 정보 가져오기
            String loggedInUserid = (String) session.getAttribute("userid");
            if (session.getAttribute("user") == null) {
                return "redirect:/login"; // 로그인 안된 경우에만 로그인 페이지로 리다이렉트
            }

            // 사용자 정보를 가져오기
            User loggedInUser = userService.findByUserid(loggedInUserid)
                                           .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 게시글 저장
            boardService.saveBoard(board, loggedInUser.getUserid()); // 로그인된 사용자의 userid를 저장
            return "redirect:/board/list"; // 게시글 목록 페이지로 리다이렉트
        }
        
        @GetMapping("/board/write")
        public String writeForm(Model model) {
            model.addAttribute("board", new Board());
            return "board/write"; // "board/write.html" 템플릿을 반환
        }
    }


