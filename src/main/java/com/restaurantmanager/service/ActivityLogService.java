package com.restaurantmanager.service;

import com.restaurantmanager.entity.ActivityLog;
import com.restaurantmanager.entity.AppUser;
import com.restaurantmanager.repository.ActivityLogRepository;
import com.restaurantmanager.repository.AppUserRepository;
import com.restaurantmanager.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final AppUserRepository appUserRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void log(UUID restaurantId, UUID actorId, String action, String description) {
        String actorName = appUserRepository.findById(actorId).map(AppUser::getName).orElse("Unknown");
        activityLogRepository.save(ActivityLog.builder()
                .restaurant(restaurantRepository.getReferenceById(restaurantId))
                .actorName(actorName)
                .action(action)
                .description(description)
                .build());
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> listRecent(UUID restaurantId, int limit) {
        return activityLogRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, PageRequest.of(0, limit));
    }
}
