package com.sportiq.backend.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DummyController {

    @GetMapping("/api/auth/me")
    @ResponseBody
    public String me() {
        return "ok";
    }

    @GetMapping("/api/sessions")
    @ResponseBody
    public String sessions() {
        return "ok";
    }
}
