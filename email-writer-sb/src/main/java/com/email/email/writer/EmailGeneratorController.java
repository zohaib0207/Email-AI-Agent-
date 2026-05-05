package com.email.email.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailGeneratorController {

    private final EmailGeneratorService service;

    @PostMapping("/generate")
    public Mono<ResponseEntity<String>> generate(@RequestBody EmailRequest request) {
        return service.generateEmailReply(request)
                .map(ResponseEntity::ok);
    }
}