package com.rubypaper.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.aspectj.weaver.Lint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "이름은 필수입니다")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "아이디는 필수 입니다.")
    @Column(nullable = false, unique = true)
    private String userid;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private LocalDate suspendedUntil;

    // 게시판 번호 (차후 수정)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Board> boards = new ArrayList<>();
    
    // 기본 생성자
    public User() {}
    
    // 생성자
    public User(String name, String userid, String password) {
        this.name = name;
        this.userid = userid;
        this.password = password;
    }
    
    // Getter와 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDate getSuspendedUntil() { return suspendedUntil; }
    public void setSuspendedUntil(LocalDate suspendedUntil) {this.suspendedUntil = suspendedUntil; }

    public List<Board> getBoards() { return boards; }
    public void setBoards(List<Board> boards) { this.boards = boards; }
}
