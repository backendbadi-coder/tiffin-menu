package com.backendbadi.menu.service;

import java.util.List;

import com.backendbadi.menu.exception.MenuItemNotFoundException;
import com.backendbadi.menu.repository.MenuItemRepository;
import com.backendbadi.menu.entity.MenuItem;
import com.backendbadi.menu.model.MenuItemRequest;
import com.backendbadi.menu.model.MenuItemResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuItemService {

    private final MenuItemRepository repository;

    public MenuItemService(MenuItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAll() {
        return repository.findAll(Sort.by("id")).stream()
            .map(MenuItemResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse findById(Long id) {
        return MenuItemResponse.from(getItem(id));
    }

    public MenuItemResponse create(MenuItemRequest request) {
        MenuItem item = new MenuItem(request.name().trim(), request.price(), request.availableOrDefault());
        return MenuItemResponse.from(repository.save(item));
    }

    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem item = getItem(id);
        item.setName(request.name().trim());
        item.setPrice(request.price());
        item.setAvailable(request.availableOrDefault());
        return MenuItemResponse.from(item);   // saved automatically when the transaction commits
    }

    public void delete(Long id) {
        repository.delete(getItem(id));
    }

    private MenuItem getItem(Long id) {
        return repository.findById(id).orElseThrow(() -> new MenuItemNotFoundException(id));
    }

    /** Available items at or below a price, cheapest first. */
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAvailableUnderPrice(int maxPrice) {
        return repository.findAll(Sort.by("price")).stream()
            .filter(MenuItem::isAvailable)
            .filter(item -> item.getPrice() <= maxPrice)
            .map(MenuItemResponse::from)
            .toList();
    }

    /** Mark an item as available or sold out. */
    public MenuItemResponse setAvailability(Long id, boolean available) {
        MenuItem item = getItem(id);
        item.setAvailable(available);
        return MenuItemResponse.from(item);   // saved when the transaction commits
    }
}
