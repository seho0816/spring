package com.rubypaper.repository;

import com.rubypaper.domain.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
	 // 모든 유니크 카테고리 조회
    @Query("SELECT DISTINCT c.category FROM Certificate c WHERE c.category IS NOT NULL AND c.category != ''")
    List<String> findDistinctCategory();

    @Query("SELECT DISTINCT c.category FROM Certificate c")
    List<String> findAllCategories();
    
    @Query("SELECT DISTINCT c.subCategory FROM Certificate c WHERE c.category = :category")
    List<String> findSubcategoriesByCategory(String category);
    
    List<Certificate> findByCategory(String category);
    // 특정 카테고리에 속하는 유니크한 서브카테고리 조회
    // ★★★ 컬럼명과 필드명 불일치 문제 해결 후, 여기에 DISTINCT 쿼리 사용 ★★★
    @Query("SELECT DISTINCT c.subCategory FROM Certificate c WHERE c.category = :category AND c.subCategory IS NOT NULL AND c.subCategory != ''")
    List<String> findDistinctSubCategoryByCategory(@Param("category") String category); // 엔티티 필드명과 일치

    // 특정 카테고리, 서브카테고리에 속하는 자격증 조회
    List<Certificate> findByCategoryAndSubCategory(String category, String subCategory); // 엔티티 필드명과 일치

    Optional<Certificate> findByCertificateName(String certificateName);
}