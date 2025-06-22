package com.rubypaper.repository;

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Certificate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 카테고리, 서브카테고리, 자격증명으로 검색 (Pageable 적용)
    Page<Board> findByCategoryAndSubcategoryAndCertificateNameContaining(String category, String subcategory, String certificateName, Pageable pageable);

    // 카테고리, 서브카테고리로 검색
    Page<Board> findByCategoryAndSubcategoryContaining(String category, String subcategory, Pageable pageable);

    // 카테고리로 검색
    Page<Board> findByCategoryContaining(String category, Pageable pageable);

    // 자격증명으로 검색 (전체 카테고리/서브카테고리에서)
    Page<Board> findByCertificateNameContaining(String certificateName, Pageable pageable);

    // 카테고리 + 자격증명으로 검색
    Page<Board> findByCategoryAndCertificateNameContaining(String category, String certificateName, Pageable pageable);

    // 서브카테고리 + 자격증명으로 검색
    Page<Board> findBySubcategoryAndCertificateNameContaining(String subcategory, String certificateName, Pageable pageable);

    //특정 카테고리의 서브카테고리 조회
    @Query("SELECT DISTINCT b.subcategory FROM Board b WHERE b.category = :category AND b.subcategory IS NOT NULL AND b.subcategory != ''")
    List<String> findDistinctSubcategoriesByCategory(@Param("category") String category);
    
    // 모든 게시글 검색 (필터 없음)
    Page<Board> findAll(Pageable pageable);
}