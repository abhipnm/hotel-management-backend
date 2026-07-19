package com.restaurantmanager.catalog;

import com.restaurantmanager.entity.FoodType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Fixed, built-in catalog of common restaurant categories and popular items, offered to
 * admin/staff as a quick-start checklist so they don't have to type out a whole menu by hand.
 * Not stored in the database — purely a static reference the "apply template" flow reads from.
 */
public final class MenuTemplateCatalog {

    private MenuTemplateCatalog() {
    }

    public record TemplateItem(String key, String name, String description, BigDecimal price, FoodType foodType) {
    }

    public record TemplateCategory(String key, String name, List<TemplateItem> items) {
    }

    public static final List<TemplateCategory> CATEGORIES = List.of(
            new TemplateCategory("STARTERS", "Starters", List.of(
                    new TemplateItem("PANEER_TIKKA", "Paneer Tikka", "Chargrilled cottage cheese marinated in spiced yogurt", new BigDecimal("220"), FoodType.VEG),
                    new TemplateItem("VEG_SPRING_ROLLS", "Veg Spring Rolls", "Crispy rolls stuffed with mixed vegetables", new BigDecimal("180"), FoodType.VEG),
                    new TemplateItem("CHICKEN_65", "Chicken 65", "Spicy deep-fried chicken bites", new BigDecimal("240"), FoodType.NON_VEG),
                    new TemplateItem("CHICKEN_WINGS", "Chicken Wings", "Tossed in a smoky BBQ glaze", new BigDecimal("260"), FoodType.NON_VEG),
                    new TemplateItem("EGG_CHILLI", "Egg Chilli", "Boiled eggs tossed in a tangy chilli sauce", new BigDecimal("190"), FoodType.EGG)
            )),
            new TemplateCategory("MAIN_COURSE", "Main Course", List.of(
                    new TemplateItem("BUTTER_CHICKEN", "Butter Chicken", "Creamy tomato-based curry with tender chicken", new BigDecimal("320"), FoodType.NON_VEG),
                    new TemplateItem("PANEER_BUTTER_MASALA", "Paneer Butter Masala", "Cottage cheese in a rich buttery tomato gravy", new BigDecimal("280"), FoodType.VEG),
                    new TemplateItem("DAL_MAKHANI", "Dal Makhani", "Slow-cooked black lentils finished with cream", new BigDecimal("220"), FoodType.VEG),
                    new TemplateItem("VEG_BIRYANI", "Veg Biryani", "Fragrant basmati rice layered with spiced vegetables", new BigDecimal("250"), FoodType.VEG),
                    new TemplateItem("CHICKEN_BIRYANI", "Chicken Biryani", "Fragrant basmati rice layered with spiced chicken", new BigDecimal("300"), FoodType.NON_VEG),
                    new TemplateItem("EGG_CURRY", "Egg Curry", "Boiled eggs simmered in a spiced onion-tomato gravy", new BigDecimal("200"), FoodType.EGG)
            )),
            new TemplateCategory("BREADS", "Breads", List.of(
                    new TemplateItem("BUTTER_NAAN", "Butter Naan", "Soft leavened bread brushed with butter", new BigDecimal("60"), FoodType.VEG),
                    new TemplateItem("GARLIC_NAAN", "Garlic Naan", "Naan topped with fresh garlic and coriander", new BigDecimal("70"), FoodType.VEG),
                    new TemplateItem("TANDOORI_ROTI", "Tandoori Roti", "Whole-wheat bread baked in the tandoor", new BigDecimal("40"), FoodType.VEG)
            )),
            new TemplateCategory("RICE_NOODLES", "Rice & Noodles", List.of(
                    new TemplateItem("VEG_FRIED_RICE", "Veg Fried Rice", "Wok-tossed rice with fresh vegetables", new BigDecimal("180"), FoodType.VEG),
                    new TemplateItem("CHICKEN_FRIED_RICE", "Chicken Fried Rice", "Wok-tossed rice with chicken and vegetables", new BigDecimal("220"), FoodType.NON_VEG),
                    new TemplateItem("HAKKA_NOODLES", "Hakka Noodles", "Stir-fried noodles with julienned vegetables", new BigDecimal("190"), FoodType.VEG)
            )),
            new TemplateCategory("DESSERTS", "Desserts", List.of(
                    new TemplateItem("GULAB_JAMUN", "Gulab Jamun", "Soft milk dumplings soaked in rose-scented syrup", new BigDecimal("90"), FoodType.VEG),
                    new TemplateItem("RASMALAI", "Rasmalai", "Cottage cheese dumplings in sweetened, cardamom milk", new BigDecimal("110"), FoodType.VEG),
                    new TemplateItem("ICE_CREAM", "Ice Cream", "Choice of vanilla, chocolate or strawberry", new BigDecimal("80"), FoodType.VEG)
            )),
            new TemplateCategory("BEVERAGES", "Beverages", List.of(
                    new TemplateItem("MASALA_CHAI", "Masala Chai", "Spiced Indian tea", new BigDecimal("40"), FoodType.VEG),
                    new TemplateItem("COLD_COFFEE", "Cold Coffee", "Chilled coffee blended with milk and ice cream", new BigDecimal("90"), FoodType.VEG),
                    new TemplateItem("FRESH_LIME_SODA", "Fresh Lime Soda", "Sweet or salted, served chilled", new BigDecimal("70"), FoodType.VEG),
                    new TemplateItem("MANGO_LASSI", "Mango Lassi", "Yogurt-based mango smoothie", new BigDecimal("100"), FoodType.VEG)
            ))
    );

    public static Optional<TemplateItem> findItem(String itemKey) {
        return CATEGORIES.stream()
                .flatMap(category -> category.items().stream())
                .filter(item -> item.key().equals(itemKey))
                .findFirst();
    }

    public static Optional<TemplateCategory> findCategoryForItem(String itemKey) {
        return CATEGORIES.stream()
                .filter(category -> category.items().stream().anyMatch(item -> item.key().equals(itemKey)))
                .findFirst();
    }
}
