package com.rubypaper.controller;

import com.rubypaper.domain.Comment;
import com.rubypaper.domain.User;
import com.rubypaper.service.CommentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CommentController {

    @Autowired
    private CommentService commentService;

    // 댓글 저장 (POST 요청)
    @PostMapping("/board/{boardId}/comment")
    public String addComment(@PathVariable Long boardId, 
                             @RequestParam String content,
                             HttpSession session, // HttpSession 주입
                             RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("user"); // 세션에서 User 객체 가져오기
        
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 후 댓글을 작성할 수 있습니다.");
            return "redirect:/login"; // 로그인 페이지로 리다이렉트 (로그인 URL이 /login 이라고 가정)
        }

        // 댓글 내용이 비어있는지 확인
        if (content == null || content.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 내용을 입력해주세요.");
            return "redirect:/board/" + boardId;
        }

        try {
            commentService.saveComment(boardId, content, loggedInUser); // 세션에서 가져온 User 객체 전달
            redirectAttributes.addFlashAttribute("successMessage", "댓글이 성공적으로 작성되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 작성 중 오류가 발생했습니다.");
            System.err.println("댓글 작성 오류: " + e.getMessage()); // 디버깅을 위한 오류 로그
        }
        return "redirect:/board/" + boardId; // 댓글 작성 후 게시글 상세 페이지로 리다이렉트
    }

    // 댓글 삭제 (POST 요청) - HTML 폼은 DELETE 메서드를 직접 지원하지 않으므로 POST로 매핑
    @PostMapping("/comment/{commentId}/delete")
    public String deleteComment(@PathVariable Long commentId,
                                HttpSession session, // HttpSession 주입
                                RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("user"); // 세션에서 User 객체 가져오기

        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "로그인 후 댓글을 삭제할 수 있습니다.");
            return "redirect:/login"; // 로그인 페이지로 리다이렉트
        }

        Long boardId = null; // 삭제 후 리다이렉트할 게시글 ID
        try {
            // 댓글을 먼저 조회하여 게시글 ID와 권한 확인에 사용할 정보를 가져옵니다.
            Comment commentToDelete = commentService.getCommentById(commentId);
            boardId = commentToDelete.getBoard().getId(); // 리다이렉트할 게시글 ID 저장

            commentService.deleteComment(commentId, loggedInUser); // 삭제 로직 호출
            redirectAttributes.addFlashAttribute("successMessage", "댓글이 삭제되었습니다.");
            return "redirect:/board/" + boardId; // 삭제 후 게시글 상세 페이지로 리다이렉트
        } catch (SecurityException e) { // 권한 없음 예외 (CommentService에서 발생)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (boardId != null) return "redirect:/board/" + boardId; // 찾은 boardId로 돌아가기
            else return "redirect:/"; // boardId를 못 찾았으면 메인으로
        } catch (IllegalArgumentException e) { // 댓글을 찾을 수 없음 예외
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/"; // 댓글 ID 잘못된 경우 메인으로
        } catch (Exception e) { // 기타 예상치 못한 오류
            redirectAttributes.addFlashAttribute("errorMessage", "댓글 삭제 중 오류가 발생했습니다.");
            System.err.println("댓글 삭제 오류: " + e.getMessage()); // 디버깅을 위한 오류 로그
            return "redirect:/"; // 기타 오류 시 메인으로
        }
    }
}