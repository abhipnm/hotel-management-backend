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
import java.util.TreeSet;
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
    public RestaurantTable create(Restaurant restaurant, CreateTableRequest request, UUID actorId) {
        if (tableRepository.existsByRestaurantIdAndTableNumberIgnoreCase(restaurant.getId(), request.tableNumber())) {
            throw new ConflictException("Table '" + request.tableNumber() + "' already exists for this restaurant");
        }
        RestaurantTable table = RestaurantTable.builder()
                .restaurant(restaurant)
                .tableNumber(request.tableNumber())
                .capacity(request.capacity())
                .qrToken(UUID.randomUUID().toString())
                .active(true)
                .build();
        table = tableRepository.save(table);
        activityLogService.log(restaurant.getId(), actorId, "TABLE_CREATED", "Created Table " + table.getTableNumber());
        return table;
    }

    /** Creates many tables in one shot (e.g. setting up a new floor) — rejects the whole batch if any table number is a duplicate. */
    @Transactional
    public List<RestaurantTable> createBulk(Restaurant restaurant, List<CreateTableRequest> requests, UUID actorId) {
        TreeSet<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (CreateTableRequest request : requests) {
            if (!seen.add(request.tableNumber())) {
                throw new ConflictException("Duplicate table number in request: '" + request.tableNumber() + "'");
            }
        }
        for (String tableNumber : seen) {
            if (tableRepository.existsByRestaurantIdAndTableNumberIgnoreCase(restaurant.getId(), tableNumber)) {
                throw new ConflictException("Table '" + tableNumber + "' already exists for this restaurant");
            }
        }
        List<RestaurantTable> tables = requests.stream()
                .map(request -> RestaurantTable.builder()
                        .restaurant(restaurant)
                        .tableNumber(request.tableNumber())
                        .capacity(request.capacity())
                        .qrToken(UUID.randomUUID().toString())
                        .active(true)
                        .build())
                .toList();
        tables = tableRepository.saveAll(tables);
        activityLogService.log(restaurant.getId(), actorId, "TABLES_BULK_CREATED",
                "Bulk-created " + tables.size() + " tables: " + String.join(", ", seen));
        return tables;
    }

    /** One-click block/unblock, separate from the full edit form — logs the change either way. */
    @Transactional
    public RestaurantTable setActive(UUID tableId, UUID restaurantId, boolean active, UUID actorId) {
        RestaurantTable table = getForRestaurant(tableId, restaurantId);
        if (table.isActive() == active) {
            return table;
        }
        table.setActive(active);
        activityLogService.log(restaurantId, actorId, active ? "TABLE_UNBLOCKED" : "TABLE_BLOCKED",
                (active ? "Unblocked Table " : "Blocked Table ") + table.getTableNumber());
        return table;
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
        table.setCapacity(request.capacity());
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

    /** Clears this waiter off every table they're currently assigned to (e.g. when their account is deactivated). */
    @Transactional
    public void unassignFromAllTables(UUID waiterId) {
        tableRepository.findByAssignedWaiterId(waiterId)
                .forEach(table -> table.setAssignedWaiter(null));
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
