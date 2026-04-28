package com.my.movierecord.content.repository;

import com.my.movierecord.content.domain.Content;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long> {}
