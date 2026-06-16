package com.api.transaction_aggregation.user.service;

import com.api.transaction_aggregation.user.dto.UserProfile;
import com.api.transaction_aggregation.user.entity.AppUser;
import com.api.transaction_aggregation.user.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository appUserRepository;

    public UserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public UserProfile getUserByUserId(Long userId) {
        Optional<AppUser> optionalUser = appUserRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            log.warn("User profile not found for user_id: {}", userId);
            return null;
        }

        AppUser user = optionalUser.get();
        return new UserProfile(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getCountry(),
                user.getIdType(),
                user.getIdReference()
        );
    }
}
