package com.rubypaper.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardDTO {
    private Long id;
    private String title;
    private String preview;
    private LocalDateTime createdAt;
    //private int viewCount;
}
