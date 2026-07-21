package com.restaurantmanager.dto.response;

import com.restaurantmanager.dto.request.ScannedMenuCategoryInput;

import java.util.List;

/**
 * Preview of what the AI read off a scanned menu photo — nothing is saved yet. The admin edits
 * this (fixing OCR mistakes, picking a food type) and re-submits it to the apply endpoint.
 */
public record ScannedMenuResponse(
        List<ScannedMenuCategoryInput> categories
) {
}
