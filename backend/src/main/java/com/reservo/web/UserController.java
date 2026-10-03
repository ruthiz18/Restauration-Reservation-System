package com.reservo.web;

import com.reservo.dto.Dtos.*;
import com.reservo.mongo.MongoRepos.NotificationLogRepository;
import com.reservo.mongo.NotificationLog;
import com.reservo.security.AuthUser;
import com.reservo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;
    private final NotificationLogRepository notificationLogs;

    @GetMapping("/users")
    public PageDto<UserDto> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return users.list(page, size);
    }

    @PatchMapping("/users/{id}/role")
    public UserDto role(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                        @Valid @RequestBody RoleRequest req) {
        return users.changeRole(actor, id, req.role());
    }

    @PatchMapping("/users/{id}/enabled")
    public UserDto enabled(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                           @RequestBody EnabledRequest req) {
        return users.setEnabled(actor, id, req.enabled());
    }

    /** Audit view of messages processed from the RabbitMQ queues. */
    @GetMapping("/notifications")
    public PageDto<NotificationLog> notifications(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return PageDto.of(notificationLogs.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100))));
    }
}
