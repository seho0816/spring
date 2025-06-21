package com.rubypaper.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String category;

    private String subcategory;

    private String certificateName;

    private String content;
    
    private String imagePath;
    
    // 사용자와의 관계 설정 (다대일 관계)
    @ManyToOne(fetch = FetchType.LAZY) // ManyToOne 관계 설정
    @JoinColumn(name = "user_id") // user_id로 매핑
    @ToString.Exclude // @Data로 인해 FetchType.LAZY에 너무 일찍 접근하여 프록시를 초기화 하려 함 그거 해결
    private User user; // 게시글 작성한 사용자
    
    @CreationTimestamp // 게시글 작성 시 현재 시간으로 설정함
    @Column(nullable = false, updatable = false) // 생성 시에만 설정되고 업데이트 불가
    private LocalDateTime createdDate;

}

