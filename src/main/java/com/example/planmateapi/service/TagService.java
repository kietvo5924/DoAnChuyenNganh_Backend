package com.example.planmateapi.service;

import com.example.planmateapi.dto.TagDto;
import com.example.planmateapi.dto.TaskDto;
import com.example.planmateapi.entity.Tag;
import com.example.planmateapi.entity.User;
import com.example.planmateapi.exception.ResourceNotFoundException;
import com.example.planmateapi.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public TagDto.Response createTag(TagDto.Request request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();

        if (tagRepository.existsByNameAndUserId(request.getName(), currentUser.getId())) {
            throw new IllegalStateException("Nhãn với tên '" + request.getName() + "' đã tồn tại.");
        }

        Tag newTag = new Tag();
        newTag.setName(request.getName());
        newTag.setColor(request.getColor());
        newTag.setUser(currentUser);

        Tag saveTag = tagRepository.save(newTag);
        return mapToResponse(saveTag);
    }

    public List<TagDto.Response> getAllTagsForCurrentUser() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        return tagRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagDto.Response updateTag(Long tagId, TagDto.Request request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhãn với ID: " + tagId));

        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền sửa nhãn này.");
        }

        tag.setName(request.getName());
        tag.setColor(request.getColor());

        Tag updatedTag = tagRepository.save(tag);
        return mapToResponse(updatedTag);
    }

    @Transactional
    public void deleteTag(Long tagId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhãn với ID: " + tagId));

        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xóa nhãn này.");
        }

        tagRepository.delete(tag);
    }

    public TagDto.Response getTagById(Long tagId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhãn với ID: " + tagId));

        if (!tag.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền xem nhãn này.");
        }

        return mapToResponse(tag);
    }

    private TagDto.Response mapToResponse(Tag tag) {
        return new TagDto.Response(tag.getId(), tag.getName(), tag.getColor());
    }
}
