package com.koreaconcrete.civilshop.category.entity;

import com.koreaconcrete.civilshop.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "categories")
public class Category extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	private Category parent;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, unique = true, length = 160)
	private String slug;

	@Column(length = 600)
	private String imageUrl;

	@Column(nullable = false)
	private Integer depth = 1;

	@Column(nullable = false)
	private Integer sortOrder = 0;

	@Column(nullable = false)
	private Boolean active = true;

	public Category(Category parent, String name, String slug, Integer depth, Integer sortOrder, Boolean active) {
		this.parent = parent;
		this.name = name;
		this.slug = slug;
		this.depth = depth;
		this.sortOrder = sortOrder;
		this.active = active;
	}
}
