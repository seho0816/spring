package com.rubypaper.controller;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder; // URLEncoder 임포트 추가
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
// import org.springframework.validation.BindingResult; // 사용하지 않으면 제거
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
// import org.springframework.web.util.UriComponentsBuilder; // 사용하지 않으면 제거

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Certificate;
import com.rubypaper.domain.User;
import com.rubypaper.service.BoardService;
// import com.rubypaper.service.CertificateService; // 사용하지 않으면 제거
import com.rubypaper.service.UserService;

import jakarta.servlet.http.HttpSession;
// import jakarta.validation.Valid; // 사용하지 않으면 제거
// import jakarta.websocket.Session; // 사용하지 않으면 제거

@Controller
@RequestMapping("/board")
public class BoardController {

	 	@Autowired
	    private BoardService boardService;
	   
	 	@Autowired
	 	private UserService userService;
	 	
	    @Value("${file.upload-dir}") // 실제 파일 저장 경로
	    private String uploadDir;

	    @Value("${file.upload-url}") // 웹 접근 URL 경로
	    private String uploadUrl;

	    
	    
	 // 게시판 목록 조회 (게시판 페이지)
	    @GetMapping("/list")
	    public String listBoards(
	            @RequestParam(value = "category", required = false) String category, // category는 항상 있을 것이므로 required = true (기본값)
	            @RequestParam(value = "subcategory", required = false) String subcategory, // subcategory도 항상 있을 것이므로 required = true (기본값)
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


	        // boardService.getCertificatesForCategoryAndSubcategory는 BoardService에 정의되어 있어야 함
	        List<Certificate> certificates = boardService.getCertificatesForCategoryAndSubcategory(category, subcategory);
	        model.addAttribute("certificates", certificates);

	        // 모델에 카테고리, 서브카테고리, 최종 자격증 정보를 추가 (Thymeleaf에서 사용)
	        model.addAttribute("selectedCategory", category);
	        model.addAttribute("selectedSubcategory", subcategory);
	        model.addAttribute("selectedCertificate", finalCertificateName); // ★finalCertificateName 사용

	        // BoardService.getSubcategoriesForCategory는 BoardService에 정의되어 있어야 함
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
	    @GetMapping("/{id}")
	    public String boardDetail(@PathVariable Long id, Model model, HttpSession session) {
	        Board board = boardService.getBoardById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));
	        model.addAttribute("board", board);


	        User loggedInUser = (User) session.getAttribute("user");
	        String loggedInUserid = (loggedInUser != null) ? loggedInUser.getUserid() : null; // User 객체에서 userid 추출
	        System.out.println("DEBUG (Controller): loggedInUserid from session: '" + loggedInUserid + "'");
	        model.addAttribute("loggedInUserid", loggedInUserid);

	        return "board/detail";
	    }

        
        @PostMapping("/save")
        public String saveBoard(@ModelAttribute Board board,
                                @RequestParam("image") MultipartFile image, 
                                HttpSession session,
                                Model model) throws Exception {

            // 로그인된 사용자 정보 가져오기
        	User loggedInUser = (User) session.getAttribute("user");
        	String loggedInUserid = (loggedInUser != null) ? loggedInUser.getUserid() : null;
            
            if (loggedInUserid == null) {
                return "redirect:/login"; // 로그인 안 된 경우 로그인 페이지로 리다이렉트
            }

            User user = userService.findByUserid(loggedInUserid)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. (ID: " + loggedInUserid + ")"));
            board.setUser(user);
            if (user == null) {
                return "redirect:/login";
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
            boardService.save(board, loggedInUserid);  // 로그인된 사용자의 userid를 저장
            
            StringBuilder redirectUrl = new StringBuilder("/board/list?");
            
            redirectUrl.append("category=");
            // 폼에서 선택된 board.category 값을 사용
            if (board.getCategory() != null && !board.getCategory().isEmpty()) {
                redirectUrl.append(URLEncoder.encode(board.getCategory(), StandardCharsets.UTF_8));
            } else {
                redirectUrl.append(URLEncoder.encode("IT", StandardCharsets.UTF_8)); // 선택되지 않았다면 기본값
            }
            redirectUrl.append("&");

            redirectUrl.append("subcategory=");
            // 폼에서 선택된 board.subcategory 값을 사용
            if (board.getSubcategory() != null && !board.getSubcategory().isEmpty()) {
                redirectUrl.append(URLEncoder.encode(board.getSubcategory(), StandardCharsets.UTF_8));
            } else {
                redirectUrl.append(URLEncoder.encode("정보통신", StandardCharsets.UTF_8)); // 선택되지 않았다면 기본값
            }
            redirectUrl.append("&");
            
            redirectUrl.append("selectedCertificate="); // listBoards는 이 파라미터 이름을 사용
            // 폼에서 선택된 board.certificateName 값을 사용
            if (board.getCertificateName() != null && !board.getCertificateName().isEmpty()) {
                redirectUrl.append(URLEncoder.encode(board.getCertificateName(), StandardCharsets.UTF_8));
            } else {
                 redirectUrl.append(URLEncoder.encode("정보처리기사", StandardCharsets.UTF_8)); // 선택되지 않았다면 기본값
            }
            
            // 디버깅을 위해 최종 리다이렉트 URL 확인
            System.out.println("DEBUG (saveBoard): Redirecting to: " + redirectUrl.toString());

            return "redirect:" + redirectUrl.toString();
        }
        
        @GetMapping("/write")
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
                    subcategories = Arrays.asList("비파괴검사", "안전관리"); // <-- 여기서 "안전관리" 서브카테고리 이름을 수정합니다. (가스기사가 아님)
                    if (subcategory != null) {
                        if (subcategory.equals("비파괴검사")) {
                            certificates = Arrays.asList("비파괴검사기술사", "누설비파괴검사기사");
                        } else if (subcategory.equals("안전관리")) { // <-- 이 부분을 "안전관리" 서브카테고리에 맞춰 수정
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
        
        @PostMapping("/delete/{id}")
        public String deleteBoard(@PathVariable Long id, HttpSession session
        		// 삭제 시 파라미터로 넘어오는 category, subcategory, certificateName은 현재 보고 있던 페이지의 필터 정보.
                // 삭제될 게시글의 정보를 사용해야 함.
                // @RequestParam(required = false) String category,
                // @RequestParam(required = false) String subcategory,
                // @RequestParam(required = false) String certificateName
                ) {
        	User loggedInUser = (User) session.getAttribute("user");
            String loggedInUserid = (loggedInUser != null) ? loggedInUser.getUserid() : null;

            if (loggedInUserid == null) {
                return "redirect:/board/" + id + "?error=notLoggedIn";
            }

            Board boardToDelete = boardService.getBoardById(id) // 삭제될 게시글 정보를 먼저 가져옴
                    .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

            boolean isAdmin = "11".equals(loggedInUserid); // "11"은 관리자 USERID
            boolean isAuthor = boardToDelete.getUser() != null && boardToDelete.getUser().getUserid().equals(loggedInUserid);

            if (isAdmin || isAuthor) {
                // 이미지 파일 삭제 로직 (기존과 동일)
                if (boardToDelete.getImagePath() != null && !boardToDelete.getImagePath().isEmpty()) {
                    try {
                        String imageFileName = boardToDelete.getImagePath().substring(uploadUrl.length()); // uploadUrl을 기준으로 파일명 추출
                        Path filePath = Paths.get(uploadDir, imageFileName);
                        Files.deleteIfExists(filePath);
                        System.out.println("DEBUG (deleteBoard): Image deleted: " + filePath.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("ERROR (deleteBoard): Failed to delete image: " + boardToDelete.getImagePath());
                    }
                }
                
                // 게시글 삭제
                boardService.deleteBoard(id);
                System.out.println("DEBUG (deleteBoard): Board with ID " + id + " deleted successfully.");

                // ★★★ 삭제된 게시글의 카테고리/서브카테고리/자격증명 정보를 사용하여 리다이렉트 URL 구성 ★★★
                StringBuilder redirectUrl = new StringBuilder("/board/list?");
                
                // 카테고리 (필수)
                redirectUrl.append("category=");
                if (boardToDelete.getCategory() != null && !boardToDelete.getCategory().isEmpty()) {
                    redirectUrl.append(URLEncoder.encode(boardToDelete.getCategory(), StandardCharsets.UTF_8));
                } else {
                    redirectUrl.append(URLEncoder.encode("IT", StandardCharsets.UTF_8)); // 기본값 설정 (listBoards와 일치)
                }
                redirectUrl.append("&");

                // 서브카테고리 (필수)
                redirectUrl.append("subcategory=");
                if (boardToDelete.getSubcategory() != null && !boardToDelete.getSubcategory().isEmpty()) {
                    redirectUrl.append(URLEncoder.encode(boardToDelete.getSubcategory(), StandardCharsets.UTF_8));
                } else {
                    redirectUrl.append(URLEncoder.encode("정보통신", StandardCharsets.UTF_8)); // 기본값 설정 (listBoards와 일치)
                }
                redirectUrl.append("&");

                // 자격증명 (선택적) - listBoards는 "selectedCertificate"로 받음
                redirectUrl.append("selectedCertificate="); // ★ 파라미터 이름을 "selectedCertificate"로 변경
                if (boardToDelete.getCertificateName() != null && !boardToDelete.getCertificateName().isEmpty()) {
                    redirectUrl.append(URLEncoder.encode(boardToDelete.getCertificateName(), StandardCharsets.UTF_8));
                } else {
                    redirectUrl.append(URLEncoder.encode("정보처리기사", StandardCharsets.UTF_8)); // 기본값 설정 (listBoards와 일치)
                }
                
                System.out.println("DEBUG (deleteBoard): Redirecting to: " + redirectUrl.toString());
                return "redirect:" + redirectUrl.toString();

            } else {
                return "redirect:/board/" + id + "?error=noPermission";
            }
        }
    }