package com.my.movierecord.movie.repository;

import com.my.movierecord.movie.domain.Content;
import com.my.movierecord.movie.domain.ContentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, ContentId> {}
