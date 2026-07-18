package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.CreateTableRequest;
import com.restaurantmanager.dto.request.UpdateTableRequest;
import com.restaurantmanager.entity.AppUser;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.RestaurantTable;
import com.restaurantmanager.exception.ConflictException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.AppUserRepository;
import com.restaurantmanager.repository.RestaurantTableRepository;
import com.restaurantmanager.util.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final AppUserRepository appUserRepository;
    private final QrCodeGenerator qrCodeGenerator;
    private final ActivityLogService activityLogService;

    @Value("${app.frontend.order-base-url:https://order.example.com}")
    private String orderBaseUrl;

    @Transactional
    public RestaurantTable create(Restaurant restaurant, CreateTableRequest request) {
        if (tableRepository.existsByRestaurantIdAndTableNumberIgnoreCase(restaurant.getId(), request.tableNumber())) {
            throw new ConflictException("Table '" + request.tableNumber() + "' already exists for this restaurant");
        }
        RestaurantTable table = RestaurantTable.builder()
                .restaurant(restaurant)
                .tableNumber(request.tableNumber())
                .qrToken(UUID.randomUUID().toString())
                .active(true)
                .build();
        return tableRepository.save(table);
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> listForRestaurant(UUID restaurantId) {
        return tableRepository.findByRestaurantIdOrderByTableNumberAsc(restaurantId);
    }

    @Transactional(readOnly = true)
    public RestaurantTable getForRestaurant(UUID tableId, UUID restaurantId) {
        return tableRepository.findByIdAndRestaurantId(tableId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found: " + tableId));
    }

    @Transactional
    public RestaurantTable update(UUID tableId, UUID restaurantId, UpdateTableRequest request) {
        RestaurantTable table = getForRestaurant(tableId, restaurantId);
        table.setTableNumber(request.tableNumber());
        table.setActive(request.active());
        return table;
    }

    /** Pass a null waiterId to unassign the table. */
    @Transactional
    public RestaurantTable assignWaiter(UUID tableId, UUID restaurantId, UUID waiterId, UUID actorId) {
        RestaurantTable table = getForRestaurant(tableId, restaurantId);
        if (waiterId == null) {
            table.setAssignedWaiter(null);
            activityLogService.log(restaurantId, actorId, "WAITER_UNASSIGNED",
                    "Unassigned waiter from Table " + table.getTableNumber());
            return table;
        }
        AppUser waiter = appUserRepository.findByIdAndRestaurantId(waiterId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + waiterId));
        table.setAssignedWaiter(waiter);
        activityLogService.log(restaurantId, actorId, "WAITER_ASSIGNED",
                "Assigned " + waiter.getName() + " to Table " + table.getTableNumber());
        return table;
    }

    @Transactional
    public void delete(UUID tableId, UUID restaurantId) {
        RestaurantTable table = getForRestaurant(tableId, restaurantId);
        tableRepository.delete(table);
    }

    @Transactional(readOnly = true)
    public RestaurantTable getByQrToken(String qrToken) {
        return tableRepository.findByQrToken(qrToken)
                .filter(RestaurantTable::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("This QR code is not valid or the table is no longer active"));
    }

    public byte[] generateQrPng(RestaurantTable table) {
        String deepLink = orderBaseUrl + "?qr=" + table.getQrToken();
        return qrCodeGenerator.generatePng(deepLink);
    }
}
