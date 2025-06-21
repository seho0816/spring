package com.rubypaper.repository;

import com.rubypaper.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    // 게시글 제목으로 검색
    List<Board> findByTitleContaining(String title);
    
    // 카테고리와 서브카테고리로 검색
    List<Board> findByCategoryAndSubcategory(String category, String subcategory);
    
    // 카테고리, 서브 카테고리, 자격증 이름으로 검색
    List<Board> findByCategoryAndSubcategoryAndCertificateName(String category, String subcategory, String certificateName);
    
    // 게시글 ID로 검색
    Optional<Board> findById(Long id);
    
    // 게시글 삭제
    void deleteById(Long id);
}
