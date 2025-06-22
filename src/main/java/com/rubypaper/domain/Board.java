package com.rubypaper.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Entity
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"user", "comments"}) // user와 comments 필드는 무한 순환 참조 방지
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String category; // IT, 안전관리

    private String subcategory; // 정보통신, 보안, 비파괴검사, 안전관리

    private String certificateName; // 정보처리기사, 정보보안기사 등

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    private int viewcount; // 조회수

    private String imagePath; // 이미지 파일 경로

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 게시글 작성자

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // DTO 대신 엔티티 직접 사용을 위한 생성자
    @Builder
    public Board(Long id, String title, String content, String category, String subcategory, String certificateName, User user, String imagePath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.subcategory = subcategory;
        this.certificateName = certificateName;
        this.user = user;
        this.viewcount = 0; // 초기 조회수는 0
        this.imagePath = imagePath;
    }

    // 게시글 수정 메서드
    public void update(String title, String content, String category, String subcategory, String certificateName, String imagePath) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.subcategory = subcategory;
        this.certificateName = certificateName;
        this.imagePath = imagePath;
    }

    // 조회수 증가 메서드
    public void increaseViewcount() {
        this.viewcount++;
    }

    // 댓글 수 (쿼리용)
    // @Formula 애노테이션은 현재 JPA Entity에 직접 적용하기 어렵고,
    // JPQL 또는 DTO Projection으로 처리하는 것이 일반적입니다.
    // 여기서는 Service 레이어에서 comments.size()를 활용하거나 DTO에 포함하는 방식으로 처리합니다.
    public int getCommentCount() {
        return this.comments != null ? this.comments.size() : 0;
    }
    
    // 본문 미리보기에 태그 없애기
    public String getPlainContent() {
        if (this.content == null) return "";
        return this.content.replaceAll("<[^>]*>", "");
    }
}