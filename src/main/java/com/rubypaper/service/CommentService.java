package com.rubypaper.service;

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Comment;
import com.rubypaper.domain.User;
import com.rubypaper.repository.BoardRepository;
import com.rubypaper.repository.CommentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 관리를 위해 임포트

import java.util.List;

@Service // 스프링 빈으로 등록
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private BoardRepository boardRepository; // 댓글 저장 시 게시글 존재 여부 확인용

    /**
     * 댓글을 저장합니다.
     * @param boardId 댓글이 달릴 게시글의 ID
     * @param content 댓글 내용
     * @param user 댓글 작성자 (세션에서 가져온 User 객체)
     * @return 저장된 댓글 엔티티
     */
    @Transactional // 메서드 실행 중 오류 발생 시 롤백
    public Comment saveComment(Long boardId, String content, User user) {
        // 게시글이 존재하는지 확인
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. (ID: " + boardId + ")"));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setUser(user);   // 댓글 작성자 설정
        comment.setBoard(board); // 댓글이 속한 게시글 설정
        return commentRepository.save(comment); // DB에 저장
    }

    /**
     * 특정 게시글의 모든 댓글을 최신순으로 조회합니다.
     * @param boardId 게시글 ID
     * @return 댓글 목록
     */
    public List<Comment> getCommentsByBoardId(Long boardId) {
        return commentRepository.findByBoardIdOrderByCreatedDateDesc(boardId);
    }

    /**
     * 댓글 ID로 댓글을 조회합니다.
     * (댓글 삭제 로직에서 boardId를 가져오기 위해 필요)
     * @param commentId 댓글 ID
     * @return 조회된 댓글 엔티티
     */
    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. (ID: " + commentId + ")"));
    }

    /**
     * 댓글을 삭제합니다. 로그인한 사용자가 댓글의 작성자인지 확인합니다.
     * @param commentId 삭제할 댓글의 ID
     * @param currentUser 현재 로그인한 사용자 (세션에서 가져온 User 객체)
     */
    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 댓글을 찾을 수 없습니다. (ID: " + commentId + ")"));

        // 댓글 삭제 권한 확인: 본인 댓글만 삭제 가능 (userid 비교)
        if (!comment.getUser().getUserid().equals(currentUser.getUserid())) {
            throw new SecurityException("댓글 삭제 권한이 없습니다."); // SecurityException 발생
        }
        commentRepository.delete(comment);
    }
}