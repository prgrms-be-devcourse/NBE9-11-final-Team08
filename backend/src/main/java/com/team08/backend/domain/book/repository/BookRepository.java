package com.team08.backend.domain.book.repository;

import com.team08.backend.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}