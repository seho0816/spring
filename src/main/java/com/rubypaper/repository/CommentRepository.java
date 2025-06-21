package com.rubypaper.repository;

import com.rubypaper.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 특정 게시글에 속한 모든 댓글을 최신순으로 정렬하여 조회
    List<Comment> findByBoardIdOrderByCreatedDateDesc(Long boardId);
}
