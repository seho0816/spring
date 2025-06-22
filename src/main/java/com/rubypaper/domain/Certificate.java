package com.rubypaper.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "certificate") // 테이블명 명시
@ToString
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category; // IT, 안전관리

    @Column(name = "SUB_CATEGORY", nullable = false)
    private String subCategory; // 정보통신, 보안, 비파괴검사, 안전관리 (Board의 subcategory와 매핑)

    @Column(nullable = false, unique = true)
    private String certificateName; // 정보처리기사, 정보보안기사 등

    @Builder
    public Certificate(String category, String subCategory, String certificateName) {
        this.category = category;
        this.subCategory = subCategory;
        this.certificateName = certificateName;
    }
}