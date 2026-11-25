package com.backendbadi.menu.repository;

import com.backendbadi.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA writes the implementation for us: save, findById, findAll, deleteById ... */
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}
