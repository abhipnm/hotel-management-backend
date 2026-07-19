package com.restaurantmanager.service;

import com.restaurantmanager.dto.request.CreateStaffRequest;
import com.restaurantmanager.dto.request.LoginRequest;
import com.restaurantmanager.dto.request.RegisterRestaurantRequest;
import com.restaurantmanager.dto.request.UpdateStaffRequest;
import com.restaurantmanager.dto.response.AuthResponse;
import com.restaurantmanager.dto.response.UserSummaryResponse;
import com.restaurantmanager.entity.AppUser;
import com.restaurantmanager.entity.Restaurant;
import com.restaurantmanager.entity.Role;
import com.restaurantmanager.exception.BadRequestException;
import com.restaurantmanager.exception.ConflictException;
import com.restaurantmanager.exception.InvalidCredentialsException;
import com.restaurantmanager.exception.ResourceNotFoundException;
import com.restaurantmanager.repository.AppUserRepository;
import com.restaurantmanager.repository.RestaurantRepository;
import com.restaurantmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RestaurantRepository restaurantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ActivityLogService activityLogService;
    private final TableService tableService;

    @Transactional
    public AuthResponse registerRestaurant(RegisterRestaurantRequest request) {
        if (restaurantRepository.existsBySlug(request.slug())) {
            throw new ConflictException("A restaurant with slug '" + request.slug() + "' already exists");
        }
        if (appUserRepository.existsByEmailIgnoreCase(request.adminEmail())) {
            throw new ConflictException("A user with email '" + request.adminEmail() + "' already exists");
        }

        Restaurant restaurant = restaurantRepository.save(Restaurant.builder()
                .name(request.restaurantName())
                .slug(request.slug())
                .address(request.address())
                .phone(request.phone())
                .active(true)
                .vegOnly(request.vegOnly())
                .build());

        AppUser admin = appUserRepository.save(AppUser.builder()
                .restaurant(restaurant)
                .name(request.adminName())
                .email(request.adminEmail())
                .passwordHash(passwordEncoder.encode(request.adminPassword()))
                .role(Role.ADMIN)
                .active(true)
                .build());

        return issueToken(admin);
    }

    @Transactional
    public AppUser createStaff(Restaurant restaurant, CreateStaffRequest request, UUID actorId) {
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with email '" + request.email() + "' already exists");
        }

        AppUser created = appUserRepository.save(AppUser.builder()
                .restaurant(restaurant)
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(true)
                .build());
        activityLogService.log(restaurant.getId(), actorId, "STAFF_CREATED",
                "Created staff account for " + created.getName() + " (" + created.getRole() + ")");
        return created;
    }

    @Transactional
    public AppUser updateStaff(UUID userId, UUID restaurantId, UUID actingUserId, UpdateStaffRequest request) {
        AppUser user = appUserRepository.findByIdAndRestaurantId(userId, restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found"));

        // An admin cannot demote or deactivate their own account — it would lock them out mid-session.
        if (user.getId().equals(actingUserId) && (!request.active() || request.role() != Role.ADMIN)) {
            throw new BadRequestException("You cannot deactivate or demote your own account");
        }

        boolean wasActive = user.isActive();
        user.setName(request.name());
        user.setRole(request.role());
        user.setActive(request.active());

        if (wasActive && !request.active()) {
            // A deactivated account can no longer log in to serve tables, so don't leave it looking "responsible" for any.
            tableService.unassignFromAllTables(user.getId());
            activityLogService.log(restaurantId, actingUserId, "STAFF_DEACTIVATED", "Deactivated staff account " + user.getName());
        } else if (!wasActive && request.active()) {
            activityLogService.log(restaurantId, actingUserId, "STAFF_REACTIVATED", "Reactivated staff account " + user.getName());
        } else {
            activityLogService.log(restaurantId, actingUserId, "STAFF_UPDATED", "Updated staff account " + user.getName());
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return issueToken(user);
    }

    private AuthResponse issueToken(AppUser user) {
        JwtService.IssuedToken issued = jwtService.generateStaffToken(user);
        UserSummaryResponse summary = new UserSummaryResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.getRestaurant().getId(), user.getRestaurant().getName(), user.isActive()
        );
        return AuthResponse.of(issued.token(), issued.expiresAt(), summary);
    }
}
