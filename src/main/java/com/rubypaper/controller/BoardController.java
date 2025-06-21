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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Certificate;
import com.rubypaper.domain.User;
import com.rubypaper.service.BoardService;
import com.rubypaper.service.CommentService;
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
	 	
	 	@Autowired
	 	private CommentService commentService;
	 	
	    @Value("${file.upload-dir}") // 실제 파일 저장 경로
	    private String uploadDir;

	    @Value("${file.upload-url}") // 웹 접근 URL 경로
	    private String uploadUrl;

	    
	    
	 // 게시판 목록 조회 (게시판 페이지)
	    @GetMapping("/list")
	    public String listBoards(
	    		@PageableDefault(size = 5, sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
	            @RequestParam(value = "category", required = false) String category, // category는 항상 있을 것이므로 required = true (기본값)
	            @RequestParam(value = "subcategory", required = false) String subcategory, // subcategory도 항상 있을 것이므로 required = true (기본값)
	            @RequestParam(value = "certificateName", required = false) String certificateNameFromUrl, // URL 초기 진입 시 (필수 아님)
	            @RequestParam(value = "selectedCertificate", required = false) String selectedCertificateFromForm, // 폼 제출 시 (필수 아님)
	            Model model) {

	    	List<Board> boards;

	        // ★★★ 모든 게시글을 조회하는 조건 추가 ★★★
	        // 만약 category, subcategory, certificateName 파라미터가 모두 없으면 모든 게시글 조회
	        if ((category == null || category.isEmpty()) &&
	            (subcategory == null || subcategory.isEmpty()) &&
	            (certificateNameFromUrl == null || certificateNameFromUrl.isEmpty()) &&
	            (selectedCertificateFromForm == null || selectedCertificateFromForm.isEmpty())) {

	            boards = boardService.getAllBoards(); // 모든 게시글 조회
	            model.addAttribute("selectedCategory", null); // 모든 게시글을 볼 때 필터가 없음을 나타냄
	            model.addAttribute("selectedSubcategory", null);
	            model.addAttribute("selectedCertificate", null);
	            model.addAttribute("subcategories", new ArrayList<String>()); // 빈 리스트
	            model.addAttribute("certificates", new ArrayList<Certificate>()); // 빈 리스트

	        } else {
	            // 기존 필터링 로직 유지
	            String finalCertificateName = null;
	            if (selectedCertificateFromForm != null && !selectedCertificateFromForm.isEmpty()) {
	                finalCertificateName = selectedCertificateFromForm;
	            } else if (certificateNameFromUrl != null && !certificateNameFromUrl.isEmpty()) {
	                finalCertificateName = certificateNameFromUrl;
	            }

	            // category나 subcategory가 null/empty일 경우 기본값 설정 (기존 listBoards 로직에 따름)
	            // 예를 들어, category가 null이면 "IT"로 간주하거나, 이 부분을 `boardService`에서 처리하도록 할 수 있습니다.
	            // 현재 코드에서는 category, subcategory가 null일 때 getSubcategoriesForCategory, getCertificatesForCategoryAndSubcategory 호출 시 NPE 발생 가능성 있음
	            // 따라서 기본값 처리를 강화합니다.
	            String effectiveCategory = (category != null && !category.isEmpty()) ? category : "IT"; // 기본 카테고리
	            String effectiveSubcategory = (subcategory != null && !subcategory.isEmpty()) ? subcategory : "정보통신"; // 기본 서브카테고리 (IT-정보통신)
	            String effectiveCertificateName = (finalCertificateName != null && !finalCertificateName.isEmpty()) ? finalCertificateName : "정보처리기사"; // 기본 자격증

	            List<Certificate> certificates = boardService.getCertificatesForCategoryAndSubcategory(effectiveCategory, effectiveSubcategory);
	            model.addAttribute("certificates", certificates);

	            model.addAttribute("selectedCategory", effectiveCategory);
	            model.addAttribute("selectedSubcategory", effectiveSubcategory);
	            model.addAttribute("selectedCertificate", effectiveCertificateName);

	            List<String> subcategories = boardService.getSubcategoriesForCategory(effectiveCategory);
	            model.addAttribute("subcategories", subcategories);


	            if (effectiveCertificateName != null && !effectiveCertificateName.isEmpty()) {
	                boards = boardService.getBoardsByCategorySubcategoryAndCertificate(effectiveCategory, effectiveSubcategory, effectiveCertificateName);
	            } else {
	                boards = boardService.getBoardsByCategoryAndSubcategory(effectiveCategory, effectiveSubcategory);
	            }
	        }
	        model.addAttribute("boards", boards);

	        return "board/list";
	    }
    // 게시글 상세 페이지
	    @GetMapping("/{id}")
<<<<<<< HEAD
	    
=======
<<<<<<< HEAD
	    
=======
	    @Transactional(readOnly = true)
>>>>>>> 0105a79d89d79f2e239b710a956f1185e5537005
>>>>>>> 21d2c90eacaf7968b7f6b207303205cef4a5ec2e
	    public String boardDetail(@PathVariable Long id, Model model, HttpSession session) {
	        Board board = boardService.getBoardById(id)
	                .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));
	        model.addAttribute("board", board);

	        model.addAttribute("comments", commentService.getCommentsByBoardId(id));

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
                                Model model,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        	
        	 User loggedInUser = (User) session.getAttribute("user");
        	 if(loggedInUser == null) {
        		 redirectAttributes.addFlashAttribute("errorMessage", "글 작성을 위해 로그인해주세요.");
                 return "redirect:/login";
        	 }
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
     // 게시글 수정 폼 요청 처리 (GET)
        @GetMapping("/modify/{id}")
        public String modifyBoardForm(@PathVariable Long id, Model model, HttpSession session) {
            // 1. 게시글 조회
            Board board = boardService.getBoardById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

            // 2. 로그인된 사용자 확인 (세션에서 가져옴)
            User loggedInUser = (User) session.getAttribute("user");
            String loggedInUserid = (loggedInUser != null) ? loggedInUser.getUserid() : null;

            
            if (loggedInUserid == null) {
                
                return "redirect:/login?redirectUrl=/board/modify/" + id; // 로그인 후 다시 수정 페이지로 오도록
            }
            
            if (!loggedInUserid.equals(board.getUser().getUserid())) {
                // 작성자가 아닌 경우
                return "redirect:/board/" + id + "?error=notAuthor"; // 상세 페이지로 리다이렉트하며 에러 전달
            }
            

            // 모델에 게시글 정보 추가 (폼에 미리 채워 넣기 위함)
            model.addAttribute("board", board);
            List<String> allCategories = Arrays.asList("IT", "안전관리");
            
            Map<String, List<String>> allSubcategoriesMap = new HashMap<>();
            allSubcategoriesMap.put("IT", Arrays.asList("정보통신", "보안"));
            allSubcategoriesMap.put("안전관리", Arrays.asList("비파괴검사", "안전관리"));

            Map<String, List<String>> allCertificatesMap = new HashMap<>();
            allCertificatesMap.put("정보통신", Arrays.asList("정보처리기사", "정보처리산업기사"));
            allCertificatesMap.put("보안", Arrays.asList("정보보안기사", "정보보안산업기사"));
            allCertificatesMap.put("비파괴검사", Arrays.asList("비파괴검사기술사", "누설비파괴검사기사"));
            allCertificatesMap.put("안전관리", Arrays.asList("가스기사", "건설안전기사")); // "가스기사", "건설안전기사"

            model.addAttribute("allCategories", allCategories); // 전체 카테고리 리스트
            model.addAttribute("allSubcategoriesMap", allSubcategoriesMap); // 전체 서브카테고리 맵
            model.addAttribute("allCertificatesMap", allCertificatesMap); // 전체 자격증 맵


            // 현재 선택된 값들도 계속 모델에 추가
            model.addAttribute("selectedCategory", board.getCategory());
            model.addAttribute("selectedSubcategory", board.getSubcategory());
            model.addAttribute("selectedCertificate", board.getCertificateName());

            // 초기 로드 시 드롭다운 값 채우기 (현재 게시글의 카테고리/서브카테고리에 맞는 리스트만)
            List<String> currentSubcategories = new ArrayList<>();
            List<String> currentCertificates = new ArrayList<>();

            if (board.getCategory() != null) {
                currentSubcategories = allSubcategoriesMap.getOrDefault(board.getCategory(), new ArrayList<>());
                if (board.getSubcategory() != null) {
                    currentCertificates = allCertificatesMap.getOrDefault(board.getSubcategory(), new ArrayList<>());
                }
            }
            model.addAttribute("subcategories", currentSubcategories); // 현재 선택된 카테고리에 맞는 서브카테고리
            model.addAttribute("certificates", currentCertificates); // 현재 선택된 서브카테고리에 맞는 자격증

            return "board/modify";
        }

        // 게시글 수정 처리 (POST) - updateBoard 메서드도 권한 검사 로직 그대로 유지
        @PostMapping("/update")
        public String updateBoard(@ModelAttribute Board board,
                                  @RequestParam("image") MultipartFile imageFile,
                                  @RequestParam(value = "deleteImage", required = false) String deleteImage,
                                  HttpSession session) throws Exception {

            User loggedInUser = (User) session.getAttribute("user");
            String loggedInUserid = (loggedInUser != null) ? loggedInUser.getUserid() : null;

            if (loggedInUserid == null) {
                return "redirect:/login"; // 로그인 안 된 경우 로그인 페이지로 리다이렉트
            }

            Board existingBoard = boardService.getBoardById(board.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + board.getId()));
            
            // 작성자 체크
            if (!loggedInUserid.equals(existingBoard.getUser().getUserid())) {
                return "redirect:/board/" + board.getId() + "?error=noPermissionToUpdate";
            }
            
            // 이미지 처리 로직
            if ("true".equals(deleteImage) && existingBoard.getImagePath() != null) {
                try {
                    String imageFileName = existingBoard.getImagePath().substring(uploadUrl.length());
                    Path filePath = Paths.get(uploadDir, imageFileName);
                    Files.deleteIfExists(filePath);
                    existingBoard.setImagePath(null);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("ERROR (updateBoard): Failed to delete old image: " + existingBoard.getImagePath());
                }
            }

            if (!imageFile.isEmpty()) {
                if (existingBoard.getImagePath() != null) {
                    try {
                        String imageFileName = existingBoard.getImagePath().substring(uploadUrl.length());
                        Path filePath = Paths.get(uploadDir, imageFileName);
                        Files.deleteIfExists(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("ERROR (updateBoard): Failed to delete previous image for replacement: " + existingBoard.getImagePath());
                    }
                }

                File uploadPath = new File(uploadDir);
                if (!uploadPath.exists()) {
                    uploadPath.mkdirs();
                }
                String originalFilename = imageFile.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                String savedFileName = UUID.randomUUID().toString() + fileExtension;
                File dest = new File(uploadPath, savedFileName);
                imageFile.transferTo(dest);
                String webAccessPath = uploadUrl + savedFileName;
                existingBoard.setImagePath(webAccessPath);
            }

            existingBoard.setTitle(board.getTitle());
            existingBoard.setContent(board.getContent());
            existingBoard.setCategory(board.getCategory());
            existingBoard.setSubcategory(board.getSubcategory());
            existingBoard.setCertificateName(board.getCertificateName());

            boardService.updateBoard(existingBoard);

            return "redirect:/board/" + board.getId();
        }
        
        @PostMapping("/delete/{id}")
        public String deleteBoard(@PathVariable Long id, HttpSession session) {

            User loggedInUser = (User) session.getAttribute("user");
            String loggedInUserid = (loggedInUser != null) ? loggedInUser.getUserid() : null;

            // ★★★ NullPointerException 방지를 위해 trim() 호출 전에 null 체크 ★★★
            String loggedInUseridTrimmed = (loggedInUserid != null) ? loggedInUserid.trim() : null;

            System.out.println("DEBUG (Controller): loggedInUserid from session (raw): '" + loggedInUserid + "'");
            System.out.println("DEBUG (Controller): loggedInUserid after trim: '" + loggedInUseridTrimmed + "'");

            // 로그인 여부 확인 (이전 코드에서 이미 있었음, 누락된 경우를 대비)
            if (loggedInUseridTrimmed == null) { // trim()된 값으로 체크
                return "redirect:/board/" + id + "?error=notLoggedIn";
            }

            Board boardToDelete = boardService.getBoardById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid board Id:" + id));

            // ★★★ isAdmin 로직에서 trim()된 값을 사용하도록 변경 ★★★
            boolean isAdmin = "11".equals(loggedInUseridTrimmed); // "11"은 관리자 USERID

            System.out.println("DEBUG (Controller): isAdmin status: " + isAdmin);

            // ★★★ isAuthor 로직도 trim()된 값을 사용하도록 변경 (일관성 유지를 위해) ★★★
            boolean isAuthor = boardToDelete.getUser() != null && boardToDelete.getUser().getUserid().equals(loggedInUseridTrimmed);

            System.out.println("DEBUG (Controller): isAuthor status: " + isAuthor);

            if (isAdmin || isAuthor) {
                // 이미지 파일 삭제 로직
                if (boardToDelete.getImagePath() != null && !boardToDelete.getImagePath().isEmpty()) {
                    try {
                        // uploadUrl이 URL 형태 (예: /images/) 이고 imagePath가 전체 URL (예: /images/abc.jpg)이라면
                        // substring 전에 imagePath가 uploadUrl로 시작하는지 확인하는 것이 안전합니다.
                        String imageFileName = boardToDelete.getImagePath();
                        if (imageFileName.startsWith(uploadUrl)) {
                             imageFileName = imageFileName.substring(uploadUrl.length());
                        } else {
                            // imagePath가 이미 파일명만 포함하거나 다른 경로인 경우 처리
                            // 이 부분은 실제 파일 저장 방식에 따라 다를 수 있습니다.
                        }

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

                // 리다이렉트 URL 구성
                StringBuilder redirectUrl = new StringBuilder("/board/list?");

                redirectUrl.append("category=");
                if (boardToDelete.getCategory() != null && !boardToDelete.getCategory().isEmpty()) {
                    redirectUrl.append(URLEncoder.encode(boardToDelete.getCategory(), StandardCharsets.UTF_8));
                } else {
                    redirectUrl.append(URLEncoder.encode("IT", StandardCharsets.UTF_8));
                }
                redirectUrl.append("&");

                redirectUrl.append("subcategory=");
                if (boardToDelete.getSubcategory() != null && !boardToDelete.getSubcategory().isEmpty()) {
                    redirectUrl.append(URLEncoder.encode(boardToDelete.getSubcategory(), StandardCharsets.UTF_8));
                } else {
                    redirectUrl.append(URLEncoder.encode("정보통신", StandardCharsets.UTF_8));
                }
                redirectUrl.append("&");

                // ★★★ 파라미터 이름을 "certificateName"으로 변경해야 합니다. ★★★ (list 페이지의 @RequestParam과 일치하도록)
                redirectUrl.append("certificateName=");
                if (boardToDelete.getCertificateName() != null && !boardToDelete.getCertificateName().isEmpty()) {
                    redirectUrl.append(URLEncoder.encode(boardToDelete.getCertificateName(), StandardCharsets.UTF_8));
                } else {
                    redirectUrl.append(URLEncoder.encode("정보처리기사", StandardCharsets.UTF_8));
                }

                System.out.println("DEBUG (deleteBoard): Redirecting to: " + redirectUrl.toString());
                return "redirect:" + redirectUrl.toString();

            } else {
                return "redirect:/board/" + id + "?error=noPermission";
            }
        }
    }