package com.example.planmateapi.controller;

import com.example.planmateapi.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class HomeController {

    private final ChatService chatService;

    @GetMapping("/test")
    public String homeCheck() {
        return "API is up and running!";
    }
}
