package com.rubypaper.controller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Certificate;
import com.rubypaper.domain.User;
import com.rubypaper.service.BoardService;
import com.rubypaper.service.CertificateService;
import com.rubypaper.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class BoardController {

	 	@Autowired
	    private BoardService boardService;
	   
	    @Value("${file.upload-dir}") // 실제 파일 저장 경로
	    private String uploadDir;

	    @Value("${file.upload-url}") // 웹 접근 URL 경로
	    private String uploadUrl;

	 // 게시판 목록 조회 (게시판 페이지)
	    @GetMapping("/board/list")
	    public String listBoards(
	            @RequestParam(value = "category") String category, // category는 항상 있을 것이므로 required = true (기본값)
	            @RequestParam(value = "subcategory") String subcategory, // subcategory도 항상 있을 것이므로 required = true (기본값)
	            @RequestParam(value = "certificateName", required = false) String certificateNameFromUrl, // URL 초기 진입 시 (필수 아님)
	            @RequestParam(value = "selectedCertificate", required = false) String selectedCertificateFromForm, // 폼 제출 시 (필수 아님)
	            Model model) {

	        // 자격증 이름 결정(폼으로 넘긴게 일단 우선임)
	        String finalCertificateName = null;
	        if (selectedCertificateFromForm != null && !selectedCertificateFromForm.isEmpty()) {
	            finalCertificateName = selectedCertificateFromForm;
	        } else if (certificateNameFromUrl != null && !certificateNameFromUrl.isEmpty()) {
	            finalCertificateName = certificateNameFromUrl;
	        }
	        
	        if (finalCertificateName == null) {
	        	finalCertificateName = "정보처리기사"; // 자격증 기본 값
	        }


	        List<Certificate> certificates = boardService.getCertificatesForCategoryAndSubcategory(category, subcategory);
	        model.addAttribute("certificates", certificates);

	        // 모델에 카테고리, 서브카테고리, 최종 자격증 정보를 추가 (Thymeleaf에서 사용)
	        model.addAttribute("selectedCategory", category);
	        model.addAttribute("selectedSubcategory", subcategory);
	        model.addAttribute("selectedCertificate", finalCertificateName); // ★finalCertificateName 사용

	        // 카테고리와 서브카테고리로 게시글 목록을 가져옵니다.
	        List<String> subcategories = boardService.getSubcategoriesForCategory(category);
	        model.addAttribute("subcategories", subcategories);

	        List<Board> boards;
	        if (finalCertificateName != null && !finalCertificateName.isEmpty()) {
	            boards = boardService.getBoardsByCategorySubcategoryAndCertificate(category, subcategory, finalCertificateName);
	        } else {
	            // 자격증 정보가 없을 경우, 카테고리/서브카테고리만으로 게시글을 조회하거나 빈 목록을 반환
	            boards = boardService.getBoardsByCategoryAndSubcategory(category, subcategory);
	        }
	        model.addAttribute("boards", boards);

	        return "board/list"; // 게시판 목록 페이지
	    }
    // 게시글 상세 페이지
	    @GetMapping("/board/{id}") // URL 경로에 게시글 ID가 포함됩니다. 예: /board/1
	    public String boardDetail(@PathVariable("id") Long id, Model model) {
	        // 게시글을 ID로 찾기
	        // .get() 대신 .orElseThrow()를 사용하여 게시글이 없을 경우 예외 처리 권장
	        Board board = boardService.getBoardById(id)
	                                  .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

	        model.addAttribute("board", board); // 찾은 게시글 객체를 모델에 담아 템플릿으로 전달
	        return "board/detail"; // board/detail.html 템플릿 반환
	    }
        
        @PostMapping("/board/save")
        public String saveBoard(@ModelAttribute Board board,
                                @RequestParam("image") MultipartFile image, 
                                HttpSession session,
                                Model model) throws Exception {

            // 로그인된 사용자 정보 가져오기
            User loggedInUserid = (User) session.getAttribute("user");
            if (loggedInUserid == null) {
                return "redirect:/login"; // 로그인 안 된 경우 로그인 페이지로 리다이렉트
            }

            
            if (!image.isEmpty()) {
                File uploadPath = new File(uploadDir);
                if (!uploadPath.exists()) {
                    uploadPath.mkdirs(); // 디렉토리가 없으면 생성
                }

                // 2. 고유한 파일명 생성 (UUID_원본파일명 또는 UUID.확장자)
                String originalFilename = image.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String savedFileName = UUID.randomUUID().toString() + fileExtension;

                // 3. 실제 파일 시스템에 저장될 경로 (ex: C:/uploads/images/고유파일명.확장자)
                File dest = new File(uploadPath, savedFileName); // uploadPath + savedFileName
                image.transferTo(dest); // MultipartFile을 실제 파일로 저장

                // 4. Board 객체에 웹에서 접근할 수 있는 URL 경로 설정 (DB에 저장될 값)
                // 예: /images/upload/고유파일명.확장자
                String webAccessPath = uploadUrl + savedFileName;
                board.setImagePath(webAccessPath); // Board 엔티티의 imagePath 필드에 저장
            } else {
                // 이미지가 첨부되지 않았을 경우 imagePath를 null로 설정하거나 기존 값 유지 (선택)
                board.setImagePath(null); // 또는 board.getImagePath()를 그대로 두어 기존 이미지를 유지
            }
            
            // 사용자 정보를 가져오기
//            User loggedInUser = userService.findByUserid(loggedInUserid)
//                                           .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 게시글 저장
            boardService.saveBoard(board, loggedInUserid.getUserid()); // 로그인된 사용자의 userid를 저장
            
            String redirectUrl = UriComponentsBuilder.fromPath("/board/list")
                    .queryParam("category", board.getCategory())
                    .queryParam("subcategory", board.getSubcategory())
                    .queryParam("certificateName", board.getCertificateName())
                    .encode(StandardCharsets.UTF_8) // 명시적으로 UTF-8 인코딩 지정
                    .build()
                    .toUriString();

            System.out.println("DEBUG: Redirecting to URL: " + redirectUrl); // 확인용

return "redirect:" + redirectUrl;
        }
        
        @GetMapping("/board/write")
        public String writeForm(@RequestParam(value = "category", required = false) String category,
                                @RequestParam(value = "subcategory", required = false) String subcategory,
                                @RequestParam(value = "certificateName", required = false) String certificateName,
                                Model model) {
        	System.out.println("selectedCertificate: " + certificateName);
            System.out.println("certificateName: " + certificateName);
            // 기본값 설정 (카테고리, 서브카테고리, 자격증)
            List<String> categories = Arrays.asList("IT", "안전관리");
            List<String> subcategories = new ArrayList<>();
            List<String> certificates = new ArrayList<>();

            // 카테고리, 서브카테고리, 자격증 값 설정
            if (category != null) {
                if (category.equals("IT")) {
                    subcategories = Arrays.asList("정보통신", "보안");
                    if (subcategory != null) {
                        if (subcategory.equals("정보통신")) {
                            certificates = Arrays.asList("정보처리기사", "정보처리산업기사");
                        } else if (subcategory.equals("보안")) {
                            certificates = Arrays.asList("정보보안기사", "정보보안산업기사");
                        }
                    }
                } else if (category.equals("안전관리")) {
                    subcategories = Arrays.asList("비파괴검사", "가스기사");
                    if (subcategory != null) {
                        if (subcategory.equals("비파괴검사")) {
                            certificates = Arrays.asList("비파괴검사기술사", "누설비파괴검사기사");
                        } else if (subcategory.equals("안전관리")) {
                            certificates = Arrays.asList("가스기사", "건설안전기사");
                        }
                    }
                }
            }

            model.addAttribute("categories", categories);
            model.addAttribute("subcategories", subcategories);
            model.addAttribute("certificates", certificates);
            model.addAttribute("selectedCategory", category);
            model.addAttribute("selectedSubcategory", subcategory);
            model.addAttribute("selectedCertificate", certificateName);

            model.addAttribute("board", new Board());
            return "board/write";
        }
    }


