package com.reservo.service;

import com.reservo.domain.Role;
import com.reservo.domain.User;
import com.reservo.dto.Dtos.PageDto;
import com.reservo.dto.Dtos.UserDto;
import com.reservo.exception.ApiException;
import com.reservo.repo.UserRepository;
import com.reservo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public PageDto<UserDto> list(int page, int size) {
        return PageDto.of(users.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("id"))).map(UserDto::from));
    }

    @Transactional
    public UserDto changeRole(AuthUser actor, Long id, Role role) {
        guardSelf(actor, id);
        User u = find(id);
        u.setRole(role);
        return UserDto.from(u);
    }

    @Transactional
    public UserDto setEnabled(AuthUser actor, Long id, boolean enabled) {
        guardSelf(actor, id);
        User u = find(id);
        u.setEnabled(enabled);
        return UserDto.from(u);
    }

    private void guardSelf(AuthUser actor, Long id) {
        if (actor.id().equals(id)) {
            throw ApiException.badRequest("You cannot change your own role or status");
        }
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    }
}
