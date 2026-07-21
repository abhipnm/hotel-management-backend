package com.restaurantmanager.service;

import com.restaurantmanager.catalog.MenuTemplateCatalog;
import com.restaurantmanager.dto.request.CreateMenuCategoryRequest;
import com.restaurantmanager.dto.request.CreateMenuItemRequest;
import com.restaurantmanager.dto.request.ScannedMenuCategoryInput;
import com.restaurantmanager.dto.request.ScannedMenuItemInput;
import com.restaurantmanager.dto.request.UpdateMenuCategoryRequest;
import com.restaurantmanager.dto.request.UpdateMenuItemRequest;
import com.restaurantmanager.dto.response.MenuCategoryResponse;
import com.restaurantmanager.dto.response.MenuItemResponse;
import com.restaurantmanager.dto.response.MenuTemplateCategoryResponse;
import com.restaurantmanager.dto.response.MenuTemplateItemResponse;
import com.restaurantmanager.entity.FoodType;
import com.restaurantmanager.entity.MenuCategory;
import com.restaurantmanager.entity.MenuItem;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.MenuCategoryRepository;
import com.restaurantmanager.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final ActivityLogService activityLogService;

    // ---- Categories ----

    @Transactional
    public MenuCategory createCategory(Restaurant restaurant, CreateMenuCategoryRequest request) {
        MenuCategory category = MenuCategory.builder()
                .restaurant(restaurant)
                .name(request.name())
                .displayOrder(request.displayOrder())
                .active(true)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<MenuCategory> listCategories(UUID restaurantId) {
        return categoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    @Transactional(readOnly = true)
    public MenuCategory getCategory(UUID categoryId, UUID restaurantId) {
        return categoryRepository.findByIdAndRestaurantId(categoryId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found: " + categoryId));
    }

    @Transactional
    public MenuCategory updateCategory(UUID categoryId, UUID restaurantId, UpdateMenuCategoryRequest request) {
        MenuCategory category = getCategory(categoryId, restaurantId);
        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());
        category.setActive(request.active());
        return category;
    }

    @Transactional
    public void deleteCategory(UUID categoryId, UUID restaurantId) {
        MenuCategory category = getCategory(categoryId, restaurantId);
        categoryRepository.delete(category);
    }

    // ---- Items ----

    @Transactional
    public MenuItem createItem(Restaurant restaurant, CreateMenuItemRequest request) {
        requireVegForVegOnly(restaurant, request.foodType());
        MenuCategory category = getCategory(request.categoryId(), restaurant.getId());
        MenuItem item = MenuItem.builder()
                .restaurant(restaurant)
                .category(category)
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .imageUrl(request.imageUrl())
                .foodType(request.foodType())
                .available(true)
                .displayOrder(request.displayOrder())
                .stockQuantity(request.stockQuantity())
                .lowStockThreshold(request.lowStockThreshold())
                .build();
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listItems(UUID restaurantId) {
        return itemRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    @Transactional(readOnly = true)
    public List<MenuItem> listAvailableItems(UUID restaurantId) {
        return itemRepository.findByRestaurantIdAndAvailableTrueOrderByDisplayOrderAsc(restaurantId);
    }

    @Transactional(readOnly = true)
    public MenuItem getItem(UUID itemId, UUID restaurantId) {
        return itemRepository.findByIdAndRestaurantId(itemId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
    }

    @Transactional
    public MenuItem updateItem(UUID itemId, UUID restaurantId, UpdateMenuItemRequest request) {
        MenuItem item = getItem(itemId, restaurantId);
        requireVegForVegOnly(item.getRestaurant(), request.foodType());
        if (!item.getCategory().getId().equals(request.categoryId())) {
            item.setCategory(getCategory(request.categoryId(), restaurantId));
        }
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setImageUrl(request.imageUrl());
        item.setFoodType(request.foodType());
        item.setAvailable(request.available());
        item.setDisplayOrder(request.displayOrder());
        item.setStockQuantity(request.stockQuantity());
        item.setLowStockThreshold(request.lowStockThreshold());
        return item;
    }

    @Transactional
    public void deleteItem(UUID itemId, UUID restaurantId) {
        MenuItem item = getItem(itemId, restaurantId);
        itemRepository.delete(item);
    }

    private void requireVegForVegOnly(Restaurant restaurant, FoodType foodType) {
        if (restaurant.isVegOnly() && foodType != FoodType.VEG) {
            throw new BadRequestException("This is a vegetarian-only restaurant; menu items must be vegetarian.");
        }
    }

    // ---- Composed views ----

    /** Public-facing menu: active categories only, available items only. */
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getPublicMenu(UUID restaurantId) {
        List<MenuCategory> categories = categoryRepository.findByRestaurantIdAndActiveTrueOrderByDisplayOrderAsc(restaurantId);
        List<MenuItem> items = itemRepository.findByRestaurantIdAndAvailableTrueOrderByDisplayOrderAsc(restaurantId);
        return composeMenu(categories, items);
    }

    /** Admin-facing menu: every category and item regardless of active/available flags. */
    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> getFullMenu(UUID restaurantId) {
        List<MenuCategory> categories = categoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
        List<MenuItem> items = itemRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
        return composeMenu(categories, items);
    }

    private List<MenuCategoryResponse> composeMenu(List<MenuCategory> categories, List<MenuItem> items) {
        Map<UUID, List<MenuItemResponse>> itemsByCategory = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getCategory().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(MenuItemResponse::from, Collectors.toList())));

        return categories.stream()
                .map(category -> MenuCategoryResponse.from(category, itemsByCategory.getOrDefault(category.getId(), List.of())))
                .collect(Collectors.toList());
    }

    // ---- Quick-start template ----

    /** The built-in catalog, with non-veg/egg items dropped for veg-only restaurants. */
    @Transactional(readOnly = true)
    public List<MenuTemplateCategoryResponse> getMenuTemplate(Restaurant restaurant) {
        return MenuTemplateCatalog.CATEGORIES.stream()
                .map(category -> {
                    List<MenuTemplateItemResponse> items = category.items().stream()
                            .filter(item -> !restaurant.isVegOnly() || item.foodType() == FoodType.VEG)
                            .map(MenuTemplateItemResponse::from)
                            .collect(Collectors.toList());
                    return new MenuTemplateCategoryResponse(category.key(), category.name(), items);
                })
                .filter(category -> !category.items().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Adds the selected template items (and their parent categories, created on first use) to the
     * restaurant's menu. Skips anything that already exists by name so re-applying is harmless.
     */
    @Transactional
    public List<MenuCategoryResponse> applyTemplate(Restaurant restaurant, List<String> itemKeys, UUID actorId) {
        Map<String, MenuCategory> resolvedCategories = new LinkedHashMap<>();
        int itemsAdded = 0;

        for (String itemKey : itemKeys) {
            MenuTemplateCatalog.TemplateItem templateItem = MenuTemplateCatalog.findItem(itemKey).orElse(null);
            if (templateItem == null || (restaurant.isVegOnly() && templateItem.foodType() != FoodType.VEG)) {
                continue;
            }
            MenuTemplateCatalog.TemplateCategory templateCategory = MenuTemplateCatalog.findCategoryForItem(itemKey).orElseThrow();

            MenuCategory category = resolvedCategories.computeIfAbsent(templateCategory.key(), key ->
                    categoryRepository.findByRestaurantIdAndNameIgnoreCase(restaurant.getId(), templateCategory.name())
                            .orElseGet(() -> categoryRepository.save(MenuCategory.builder()
                                    .restaurant(restaurant)
                                    .name(templateCategory.name())
                                    .displayOrder(categoryRepository.countByRestaurantId(restaurant.getId()))
                                    .active(true)
                                    .build())));

            if (itemRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), templateItem.name())) {
                continue;
            }
            itemRepository.save(MenuItem.builder()
                    .restaurant(restaurant)
                    .category(category)
                    .name(templateItem.name())
                    .description(templateItem.description())
                    .price(templateItem.price())
                    .foodType(templateItem.foodType())
                    .available(true)
                    .displayOrder(0)
                    .build());
            itemsAdded++;
        }

        if (itemsAdded > 0) {
            activityLogService.log(restaurant.getId(), actorId, "MENU_TEMPLATE_APPLIED",
                    "Added " + itemsAdded + " item" + (itemsAdded == 1 ? "" : "s") + " from the menu template");
        }
        return getFullMenu(restaurant.getId());
    }

    // ---- AI menu scan ----

    /**
     * Adds the admin-reviewed items extracted from a scanned menu photo. Mirrors applyTemplate:
     * finds-or-creates categories by name and skips items that already exist by name.
     */
    @Transactional
    public List<MenuCategoryResponse> applyScannedMenu(Restaurant restaurant, List<ScannedMenuCategoryInput> categories, UUID actorId) {
        Map<String, MenuCategory> resolvedCategories = new LinkedHashMap<>();
        int itemsAdded = 0;

        for (ScannedMenuCategoryInput categoryInput : categories) {
            MenuCategory category = resolvedCategories.computeIfAbsent(categoryInput.name().toLowerCase(), key ->
                    categoryRepository.findByRestaurantIdAndNameIgnoreCase(restaurant.getId(), categoryInput.name())
                            .orElseGet(() -> categoryRepository.save(MenuCategory.builder()
                                    .restaurant(restaurant)
                                    .name(categoryInput.name())
                                    .displayOrder(categoryRepository.countByRestaurantId(restaurant.getId()))
                                    .active(true)
                                    .build())));

            for (ScannedMenuItemInput itemInput : categoryInput.items()) {
                if (restaurant.isVegOnly() && itemInput.foodType() != FoodType.VEG) {
                    continue;
                }
                if (itemRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), itemInput.name())) {
                    continue;
                }
                itemRepository.save(MenuItem.builder()
                        .restaurant(restaurant)
                        .category(category)
                        .name(itemInput.name())
                        .description(itemInput.description())
                        .price(itemInput.price())
                        .foodType(itemInput.foodType())
                        .available(true)
                        .displayOrder(0)
                        .build());
                itemsAdded++;
            }
        }

        if (itemsAdded > 0) {
            activityLogService.log(restaurant.getId(), actorId, "MENU_SCAN_APPLIED",
                    "Added " + itemsAdded + " item" + (itemsAdded == 1 ? "" : "s") + " from a scanned menu");
        }
        return getFullMenu(restaurant.getId());
    }
}
