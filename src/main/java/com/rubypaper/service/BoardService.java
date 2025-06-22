package com.rubypaper.service;

import com.rubypaper.domain.Board;
import com.rubypaper.domain.Certificate;
import com.rubypaper.domain.User;
import com.rubypaper.repository.BoardRepository;
import com.rubypaper.repository.CertificateRepository;
import com.rubypaper.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {
	
	@Value("${file.upload-dir}")
    private String uploadDir;
    @Value("${file.upload-url}")
    private String uploadUrl;

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository; // CertificateRepository 주입

    // 게시글 작성
    @Transactional
    public Long saveBoard(String title, String content, String category, String subcategory, String certificateName, String userId, MultipartFile imageFile) throws IOException {
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        String imagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            // 업로드 디렉토리가 없으면 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = imageFile.getOriginalFilename();
            // 파일명 중복을 피하기 위해 UUID 추가
            String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(storedFilename);
            
            // Files.copy 대신 transferTo 사용 (더 일반적으로 사용되며, InputStream 처리 불필요)
            imageFile.transferTo(filePath.toFile());
            
            // ★ 여기를 수정합니다. file.upload-url에 지정된 경로를 사용합니다. ★
            imagePath = uploadUrl + storedFilename;
        }

        Board board = Board.builder()
                .title(title)
                .content(content)
                .category(category)
                .subcategory(subcategory)
                .certificateName(certificateName)
                .user(user)
                .imagePath(imagePath)
                .build();

        return boardRepository.save(board).getId();
    }
    
    // 게시글 수정
    @Transactional
    public Board updateBoard(Long id, String title, String content, String category, String subcategory, String certificateName, MultipartFile newImageFile, boolean deleteExistingImage) throws IOException {
        Board existingBoard = boardRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Board not found with id: " + id));

        // 작성자 필드 업데이트 방지: user 필드는 그대로 유지
        existingBoard.setTitle(title);
        existingBoard.setContent(content);
        existingBoard.setCategory(category);
        existingBoard.setSubcategory(subcategory);
        existingBoard.setCertificateName(certificateName);

        // 기존 이미지 처리 로직
        // 1. 기존 이미지 삭제 요청이 있거나 (deleteExistingImage=true)
        // 2. 새로운 이미지가 업로드되면서 기존 이미지를 대체해야 하는 경우
        if ((deleteExistingImage || (newImageFile != null && !newImageFile.isEmpty()))
             && existingBoard.getImagePath() != null && !existingBoard.getImagePath().isEmpty()) {
            
            // 기존 파일 삭제
            String oldFileName = existingBoard.getImagePath().substring(uploadUrl.length()); // URL에서 파일명 추출
            Path oldFilePath = Paths.get(uploadDir, oldFileName);
            Files.deleteIfExists(oldFilePath);
            existingBoard.setImagePath(null); // DB 경로도 null로 설정
        }

        // 새로운 이미지 업로드 로직
        if (newImageFile != null && !newImageFile.isEmpty()) {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = newImageFile.getOriginalFilename();
            String storedFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(storedFilename);
            newImageFile.transferTo(filePath.toFile());

            existingBoard.setImagePath(uploadUrl + storedFilename); // 새 이미지 URL 저장
        }

        return boardRepository.save(existingBoard);
    }

    // 게시글 삭제
    @Transactional
    public void deleteBoard(Long id) throws IOException {
        Board boardToDelete = boardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Board not found with id: " + id));

        // 이미지 파일 삭제 로직 추가
        if (boardToDelete.getImagePath() != null && !boardToDelete.getImagePath().isEmpty()) {
            String imageFileName = boardToDelete.getImagePath().substring(uploadUrl.length()); // URL에서 파일명 추출
            Path filePath = Paths.get(uploadDir, imageFileName);
            Files.deleteIfExists(filePath);
        }
        boardRepository.deleteById(id);
    }

    public List<String> getAllCategories() {
        return certificateRepository.findAllCategories();
    }

    public List<String> getSubcategoriesByCategory(String category) {
        return certificateRepository.findSubcategoriesByCategory(category);
    }

    public List<Certificate> getAllCertificates() {
        return certificateRepository.findAll();
    }
    
    public List<Certificate> getCertificatesByCategoryAndSubcategory(String category, String subcategory) {
        return certificateRepository.findByCategoryAndSubCategory(category, subcategory);
    }

    public List<Certificate> getCertificatesByCategory(String category) {
        return certificateRepository.findByCategory(category);
    }


    // 단일 게시글 조회 및 조회수 증가
    @Transactional
    public Board getBoardById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Board not found with id: " + id));
        board.increaseViewcount(); // 조회수 증가
        return board;
    }

    // 게시글 목록 조회 (필터링 및 페이징)
    public Page<Board> getBoards(String category, String subCategory, String certificateName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        if (category != null && !category.isEmpty()) {
            if (subCategory != null && !subCategory.isEmpty()) {
                if (certificateName != null && !certificateName.isEmpty()) {
                    // 카테고리, 서브카테고리, 자격증명 모두 필터링
                    return boardRepository.findByCategoryAndSubcategoryAndCertificateNameContaining(category, subCategory, certificateName, pageable);
                } else {
                    // 카테고리, 서브카테고리 필터링
                    return boardRepository.findByCategoryAndSubcategoryContaining(category, subCategory, pageable);
                }
            } else if (certificateName != null && !certificateName.isEmpty()) {
                // 카테고리, 자격증명 필터링 (서브카테고리 전체)
                return boardRepository.findByCategoryAndCertificateNameContaining(category, certificateName, pageable);
            } else {
                // 카테고리만 필터링
                return boardRepository.findByCategoryContaining(category, pageable);
            }
        } else if (certificateName != null && !certificateName.isEmpty()) {
            // 자격증명만 필터링 (카테고리, 서브카테고리 전체)
            return boardRepository.findByCertificateNameContaining(certificateName, pageable);
        } else {
            // 필터링 없음
            return boardRepository.findAll(pageable);
        }
    }

}