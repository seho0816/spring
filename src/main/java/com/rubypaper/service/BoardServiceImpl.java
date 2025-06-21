//package com.rubypaper.service;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import com.rubypaper.domain.Board;
//import com.rubypaper.domain.User;
//import com.rubypaper.repository.BoardRepository;
//import com.rubypaper.repository.CertificateRepository;
//import com.rubypaper.repository.UserRepository;
//
////@Service
//public class BoardServiceImpl implements BoardService{
//	 @Autowired
//	    private BoardRepository boardRepository;
//	    
//	    @Autowired
//	    private CertificateRepository certificateRepository;
//	    
//	    @Autowired
//	    private UserRepository userRepository;
//	    
//	    @Autowired
//	    private UserService userService;
//
//	    // 게시글 저장
//	 // 모든 게시글을 가져오는 메서드
//	    @Override
//	    public List<Board> getAllBoards() {
//	        return boardRepository.findAll();
//	    }
//
//	    @Override
//	    // 카테고리와 서브카테고리로 게시글 목록을 가져오는 메서드
//	    public List<Board> getBoardsByCategoryAndSubcategory(String category, String subcategory) {
//	        return boardRepository.findByCategoryAndSubcategory(category, subcategory);
//	    }
//
//	    @Override
//	    // 게시글을 ID로 찾는 메서드
//	    public Optional<Board> getBoardById(Long id) {
//	        return boardRepository.findById(id);
//	    }
//
//	    // 게시글 저장
//	    @Transactional
//	    @Override
//	    public void saveBoard(Board board, String userId) {
//	    	User user = userService.findByUserid(userId)
//	                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
//
//	    			// 	User 객체를 Board 객체에 설정
//	    			board.setUser(user);
//	    			boardRepository.save(board); // 게시글 저장
//	    }
//
//	    // 게시글 삭제
//	    @Transactional
//	    @Override
//	    public void deleteBoard(Long id) {
//	        boardRepository.deleteById(id);
//	    }
//	    
//	    @Override
//	    public List<String> getSubcategoriesForCategory(String category) {
//	        // 카테고리에 맞는 서브카테고리 목록을 반환
//	        List<String> subcategories = new ArrayList<>();
//
//	        if ("IT".equals(category)) {
//	            subcategories.add("정보통신");
//	            subcategories.add("보안");
//	        } else if ("안전관리".equals(category)) {
//	            subcategories.add("비파괴검사");
//	            subcategories.add("가스기사");
//	        } else if ("보안".equals(category)) {
//	            subcategories.add("네트워크 보안");
//	            subcategories.add("정보 보안");
//	        } else {
//	            subcategories.add("기타");
//	        }
//
//	        return subcategories;
//	    }
//	    
//	    @Override
//	    public List<String> getCertificatesForCategoryAndSubcategory(String category, String subcategory) {
//	        List<String> certificates = new ArrayList<>();
//	        
//	        // 카테고리와 서브카테고리 조건에 맞는 자격증 리스트를 가져오는 로직 추가
//	        if ("IT".equals(category)) {
//	            if ("정보통신".equals(subcategory)) {
//	                certificates.add("정보처리기사");
//	                certificates.add("정보보안기사");
//	            } else if ("보안".equals(subcategory)) {
//	                certificates.add("정보보안기사");
//	                certificates.add("네트워크 보안기사");
//	            }
//	        } else if ("안전관리".equals(category)) {
//	            if ("비파괴검사".equals(subcategory)) {
//	                certificates.add("비파괴검사기술사");
//	            } else if ("가스기사".equals(subcategory)) {
//	                certificates.add("가스기사");
//	            }
//	        }
//
//	        // 그 외 다른 카테고리와 서브카테고리도 처리 가능
//	        return certificates;
//	    }
//}
