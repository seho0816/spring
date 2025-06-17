package com.rubypaper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.rubypaper.repository.CertificateRepository;
import com.rubypaper.service.CertificateService;

@Controller
public class CertificateController {

	@Autowired
    private CertificateRepository certificateRepository;

	@Autowired
	private CertificateService certificateService;
	
    // 자격증 목록을 가져와서 뷰에 전달
    @GetMapping("/")
    public String getCertificates(Model model) {
        // 데이터베이스에서 모든 자격증을 가져오기
        model.addAttribute("certificates", certificateRepository.findAll());
        return "main";
    }
    
    @GetMapping("/main")
    public String mainPage(Model model) {
        // 자격증 데이터를 모델에 추가
        model.addAttribute("certificates", certificateService.getAllCertificates());
        return "main";  // main.html을 반환
    }
    
}