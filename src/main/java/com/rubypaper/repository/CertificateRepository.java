package com.rubypaper.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rubypaper.domain.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, Long>{
	List<Certificate> findAll();
	
	
}
