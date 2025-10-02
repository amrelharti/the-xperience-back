package com.thexperience.backend.users;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MeController {
    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public Object me(Authentication auth) {
        if (auth == null || auth.getName() == null) return Map.of("authenticated", false);
        return Map.of("authenticated", true, "userId", auth.getName());
    }
}
