package com.rubypaper.controller;

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Certificate;
import com.rubypaper.domain.User; // User 도메인 import 추가
import com.rubypaper.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    // 세션에서 로그인된 사용자 ID를 가져오는 헬퍼 메서드
    private String getLoggedInUserId(HttpSession session) {
        
        if (session == null) {
            return null;
        }
        Object userObject = session.getAttribute("user"); // "user"라는 이름으로 User 객체를 가져옴
        if (userObject instanceof User) {
            return ((User) userObject).getUserid(); // User 객체에서 userid 추출
        }
        return null; // 로그인되어 있지 않거나 User 객체가 아닌 경우
    }

    // 게시글 목록 조회 (페이징 및 필터링 적용)
    @GetMapping("/list")
    public String listBoards(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "category", required = false) String selectedCategory,
            @RequestParam(value = "subCategory", required = false) String selectedSubCategory,
            @RequestParam(value = "certificateName", required = false) String selectedCertificate,
            Model model) {

        Page<Board> boardPage = boardService.getBoards(selectedCategory, selectedSubCategory, selectedCertificate, page, size);

        int totalPages = boardPage.getTotalPages();
        int currentPage = boardPage.getNumber(); // 0-indexed
        int startPage;
        int endPage;

        if (totalPages <= 10) {
            startPage = 0;
            endPage = totalPages - 1;
        } else {
            startPage = Math.max(0, currentPage - 4);
            endPage = Math.min(totalPages - 1, currentPage + 5);
            if (endPage - startPage < 9) { // Ensure always 10 pages if possible
                startPage = Math.max(0, endPage - 9);
            }
        }

        List<Integer> pageNumbers = IntStream.rangeClosed(startPage, endPage)
                .boxed()
                .collect(Collectors.toList());

        model.addAttribute("boards", boardPage);
        model.addAttribute("pageNumbers", pageNumbers);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);


        // 필터링에 사용된 값들을 모델에 추가하여 드롭다운 상태 유지
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("selectedSubCategory", selectedSubCategory);
        model.addAttribute("selectedCertificate", selectedCertificate);

        model.addAttribute("categories", boardService.getAllCategories());
        List<String> subcategories = new ArrayList<>();
        List<Certificate> certificates = new ArrayList<>();
        
        // 만약 selectedCategory가 있다면 해당 서브카테고리도 미리 로드하여 모델에 추가합니다.
        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            model.addAttribute("subcategories", boardService.getSubcategoriesByCategory(selectedCategory));
            // 만약 selectedSubCategory도 있다면 해당 자격증도 미리 로드하여 모델에 추가합니다.
            if (selectedSubCategory != null && !selectedSubCategory.isEmpty()) {
                model.addAttribute("certificates", boardService.getCertificatesByCategoryAndSubcategory(selectedCategory, selectedSubCategory));
            }
        }
        
        
        return "board/list";
    }

    // 게시글 상세 조회
    @GetMapping("/{id}")
    public String getBoardDetail(@PathVariable Long id, Model model, HttpServletRequest request) {
        Board board = boardService.getBoardById(id);
        model.addAttribute("board", board);
        model.addAttribute("comments", board.getComments());

        HttpSession session = request.getSession(false);
        // ★★★ getLoggedInUserId 헬퍼 메서드 사용 ★★★
        String loggedInUserid = getLoggedInUserId(session);
        if (loggedInUserid != null) {
            model.addAttribute("loggedInUserid", loggedInUserid);
        }
        return "board/detail";
    }

    // 게시글 작성 페이지로 이동
    @GetMapping("/write")
    public String showWriteForm(
            @ModelAttribute("board") Board board,
            @RequestParam(value = "category", required = false) String selectedCategory,
            @RequestParam(value = "subCategory", required = false) String selectedSubCategory,
            @RequestParam(value = "certificateName", required = false) String selectedCertificate,
            Model model, HttpSession session) {

        // ★★★ getLoggedInUserId 헬퍼 메서드 사용 ★★★
        String loggedInUserid = getLoggedInUserId(session);
        if (loggedInUserid == null) {
            return "redirect:/login?redirectURL=/board/write";
        }

        List<String> allCategories = boardService.getAllCategories();
        model.addAttribute("categories", allCategories);

        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            List<String> subcategories = boardService.getSubcategoriesByCategory(selectedCategory);
            model.addAttribute("subcategories", subcategories);
            if (selectedSubCategory != null && !selectedSubCategory.isEmpty()) {
                List<Certificate> certificates = boardService.getCertificatesByCategoryAndSubcategory(selectedCategory, selectedSubCategory);
                model.addAttribute("certificates", certificates);
            }
        }

        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("selectedSubCategory", selectedSubCategory);
        model.addAttribute("selectedCertificate", selectedCertificate);

        if (board.getTitle() == null) {
            model.addAttribute("board", new Board());
        }

        return "board/write";
    }

    // 게시글 저장
    @PostMapping("/save")
    public String saveBoard(
            @ModelAttribute("board") Board board,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // ★★★ getLoggedInUserId 헬퍼 메서드 사용 ★★★
        String loggedInUserid = getLoggedInUserId(session);
        if (loggedInUserid == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 후 게시글을 작성할 수 있습니다.");
            return "redirect:/login";
        }

        try {
            boardService.saveBoard(board.getTitle(), board.getContent(), board.getCategory(), board.getSubcategory(), board.getCertificateName(), loggedInUserid, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "게시글이 성공적으로 작성되었습니다.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "파일 업로드 중 오류가 발생했습니다.");
            return "redirect:/board/write";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/board/write";
        }

        String redirectUrl = "/board/list?category=" + URLEncoder.encode(board.getCategory(), StandardCharsets.UTF_8) +
                "&subCategory=" + URLEncoder.encode(board.getSubcategory(), StandardCharsets.UTF_8) +
                "&certificateName=" + URLEncoder.encode(board.getCertificateName(), StandardCharsets.UTF_8);

return "redirect:" + redirectUrl;
    }

    // 게시글 수정 페이지로 이동
    @GetMapping("/modify/{id}")
    public String showModifyForm(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // ★★★ getLoggedInUserId 헬퍼 메서드 사용 ★★★
        String loggedInUserid = getLoggedInUserId(session);
        if (loggedInUserid == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 후 수정할 수 있습니다.");
            return "redirect:/login?redirectURL=/board/modify/" + id;
        }

        Board board = boardService.getBoardById(id);

        System.out.println("DEBUG: Entering modify form for Board ID: " + id);
        if (board != null) {
            System.out.println("DEBUG: Board loaded into model for modify form. Board ID: " + board.getId());
        } else {
            System.out.println("DEBUG: Board is null for ID: " + id);
        }
        
        if (!loggedInUserid.equals(board.getUser().getUserid())) {
            redirectAttributes.addFlashAttribute("errorMessage", "작성자만 게시글을 수정할 수 있습니다.");
            return "redirect:/board/" + id;
        }

        model.addAttribute("board", board);

        List<String> allCategories = boardService.getAllCategories();
        model.addAttribute("categories", allCategories);

        if (board.getCategory() != null && !board.getCategory().isEmpty()) {
            List<String> subcategories = boardService.getSubcategoriesByCategory(board.getCategory());
            model.addAttribute("subcategories", subcategories);
        }

        if (board.getCategory() != null && !board.getCategory().isEmpty() && board.getSubcategory() != null && !board.getSubcategory().isEmpty()) {
            List<Certificate> certificates = boardService.getCertificatesByCategoryAndSubcategory(board.getCategory(), board.getSubcategory());
            model.addAttribute("certificates", certificates);
        }

        model.addAttribute("selectedCategory", board.getCategory());
        model.addAttribute("selectedSubCategory", board.getSubcategory());
        model.addAttribute("selectedCertificate", board.getCertificateName());
        model.addAttribute("plainContent", board.getPlainContent());
        
        return "board/modify";
    }


    // 게시글 업데이트
    @PostMapping("/update")
    public String updateBoard(
            @ModelAttribute("board") Board board,
            @RequestParam(value = "image", required = false) MultipartFile newImageFile,
            @RequestParam(value = "deleteImage", defaultValue = "false") boolean deleteExistingImage,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
    	System.out.println("Received Board ID for update: " + board.getId()); // 로그 추가
        // ★★★ getLoggedInUserId 헬퍼 메서드 사용 ★★★
        String loggedInUserid = getLoggedInUserId(session);
        if (loggedInUserid == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 후 수정할 수 있습니다.");
            return "redirect:/login";
        }

        try {
            Board existingBoard = boardService.getBoardById(board.getId());
            if (!loggedInUserid.equals(existingBoard.getUser().getUserid()) && !loggedInUserid.equals("11")) {
                redirectAttributes.addFlashAttribute("errorMessage", "작성자 또는 관리자만 게시글을 수정할 수 있습니다.");
                return "redirect:/board/" + board.getId();
            }

            boardService.updateBoard(board.getId(), board.getTitle(), board.getContent(), board.getCategory(),
                                     board.getSubcategory(), board.getCertificateName(), newImageFile, deleteExistingImage);
            redirectAttributes.addFlashAttribute("successMessage", "게시글이 성공적으로 수정되었습니다.");
        } catch (jakarta.persistence.EntityNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 게시글을 찾을 수 없습니다.");
            return "redirect:/board/list";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "파일 업로드/삭제 중 오류가 발생했습니다.");
            return "redirect:/board/modify/" + board.getId();
        }

        return "redirect:/board/" + board.getId();
    }

    // 게시글 삭제
    @PostMapping("/delete/{id}")
    public String deleteBoard(
            @PathVariable Long id,
            @RequestParam(value = "category", required = false) String category, // JavaScript에서 넘어온 카테고리
            @RequestParam(value = "subCategory", required = false) String subCategory, // JavaScript에서 넘어온 서브카테고리
            @RequestParam(value = "certificateName", required = false) String certificateName, // JavaScript에서 넘어온 자격증명
            @RequestParam(value = "page", defaultValue = "0") int page, // Paging 정보 추가
            @RequestParam(value = "size", defaultValue = "10") int size, // Paging 정보 추가
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("user");
        String loggedInUserid = (loggedInUser != null) ? loggedInUser.getUserid() : null;
        String loggedInUseridTrimmed = (loggedInUserid != null) ? loggedInUserid.trim() : null;

        if (loggedInUseridTrimmed == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 후 삭제할 수 있습니다.");
            return "redirect:/login?redirectURL=/board/" + id;
        }

        Board boardToDelete = boardService.getBoardById(id);

        if (boardToDelete == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않는 게시글입니다.");
            return "redirect:/board/list";
        }

        boolean isAdmin = "11".equals(loggedInUseridTrimmed);
        boolean isAuthor = boardToDelete.getUser() != null && boardToDelete.getUser().getUserid().equals(loggedInUseridTrimmed);

        if (!isAdmin && !isAuthor) {
            redirectAttributes.addFlashAttribute("errorMessage", "작성자 또는 관리자만 게시글을 삭제할 수 있습니다.");
            return "redirect:/board/" + id;
        }

        try {
            // 이미지 파일 삭제 로직 (기존 코드 그대로 유지)
            // ... (생략) ...
            // 이 부분은 uploadDir, uploadUrl 변수가 컨트롤러 클래스 레벨에 정의되어 있어야 합니다.
            // Paths.get(uploadDir, imageFileName) 이 부분이 실행되려면 이 변수들이 필요합니다.
            // 만약 없다면, 해당 로직은 BoardService로 옮기는 것이 더 좋습니다.
            
            boardService.deleteBoard(id);
            redirectAttributes.addFlashAttribute("successMessage", "게시글이 성공적으로 삭제되었습니다.");
        } catch (IOException e) {
            System.err.println("Error deleting board image file: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/board/" + id; // 오류 시 원래 페이지로 돌아가되 필터는 적용하지 않음
        } catch (Exception e) { // 기타 예외 처리
            System.err.println("Error deleting board: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "게시글 삭제 중 알 수 없는 오류가 발생했습니다.");
            return "redirect:/board/" + id;
        }

        // ★★★ 삭제 후 리다이렉트 URL 구성 (JavaScript에서 넘겨받은 파라미터 사용) ★★★
        StringBuilder redirectUrl = new StringBuilder("redirect:/board/list");
        boolean isFirstParam = true;

        // JavaScript에서 넘겨받은 category, subCategory, certificateName 사용
        if (category != null && !category.isEmpty()) {
            redirectUrl.append(isFirstParam ? "?" : "&").append("category=").append(URLEncoder.encode(category, StandardCharsets.UTF_8));
            isFirstParam = false;
        }
        if (subCategory != null && !subCategory.isEmpty()) {
            redirectUrl.append(isFirstParam ? "?" : "&").append("subCategory=").append(URLEncoder.encode(subCategory, StandardCharsets.UTF_8));
            isFirstParam = false;
        }
        if (certificateName != null && !certificateName.isEmpty()) {
            redirectUrl.append(isFirstParam ? "?" : "&").append("certificateName=").append(URLEncoder.encode(certificateName, StandardCharsets.UTF_8));
            isFirstParam = false;
        }

        // ★★★ 페이징 정보 추가 ★★★
        // JavaScript에서 넘겨받은 page, size 사용
        if (page > 0 || size != 10) { // 기본값(0, 10)이 아닌 경우에만 명시적으로 추가
            redirectUrl.append(isFirstParam ? "?" : "&").append("page=").append(page);
            isFirstParam = false;
            redirectUrl.append(isFirstParam ? "?" : "&").append("size=").append(size);
            isFirstParam = false;
        }


        System.out.println("DEBUG (deleteBoard): Redirecting to: " + redirectUrl.toString());
        return redirectUrl.toString();
    }

    // API 엔드포인트: 특정 카테고리에 속하는 서브카테고리 목록 반환
    @GetMapping("/api/subcategories")
    @ResponseBody
    public ResponseEntity<List<String>> getSubcategories(@RequestParam String category) {
        List<String> subcategories = boardService.getSubcategoriesByCategory(category);
        return new ResponseEntity<>(subcategories, HttpStatus.OK);
    }

    // API 엔드포인트: 특정 카테고리와 서브카테고리에 속하는 자격증 목록 반환
    @GetMapping("/api/certificates")
    @ResponseBody
    public ResponseEntity<List<Certificate>> getCertificates(@RequestParam String category, @RequestParam String subCategory) {
        List<Certificate> certificates = boardService.getCertificatesByCategoryAndSubcategory(category, subCategory);
        return new ResponseEntity<>(certificates, HttpStatus.OK);
    }
}