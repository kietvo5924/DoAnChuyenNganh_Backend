package com.example.planmateapi.controller;

import com.example.planmateapi.dto.TagDto;
import com.example.planmateapi.service.TagService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "Tag API")
@SecurityRequirement(name = "bearerAuth")
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<TagDto.Response> createTag(@Valid @RequestBody TagDto.Request request) {
        TagDto.Response createdTag = tagService.createTag(request);
        return new ResponseEntity<>(createdTag, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TagDto.Response>> getAllTags() {
        List<TagDto.Response> tags = tagService.getAllTagsForCurrentUser();
        return ResponseEntity.ok(tags);
    }

    @PutMapping("/{tagId}")
    public ResponseEntity<TagDto.Response> updateTag(@PathVariable Long tagId, @Valid @RequestBody TagDto.Request request) {
        TagDto.Response updatedTag = tagService.updateTag(tagId, request);
        return ResponseEntity.ok(updatedTag);
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tagId}")
    public ResponseEntity<TagDto.Response> getTagById(@PathVariable Long tagId) {
        TagDto.Response tag = tagService.getTagById(tagId);
        return ResponseEntity.ok(tag);
    }
}
