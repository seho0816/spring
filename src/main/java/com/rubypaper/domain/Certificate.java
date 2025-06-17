package com.rubypaper.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Certificate {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String certificateName;
	private String category;
	private String subCategory;
	
	public Certificate() {
		
	}
	
	 public Certificate(String certificateName, String category, String subcategory) {
	        this.certificateName = certificateName;
	        this.category = category;
	        this.subCategory = subcategory;
	    }
}
