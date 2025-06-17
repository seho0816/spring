package com.rubypaper.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

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
    private User user; // 게시글 작성한 사용자

}

