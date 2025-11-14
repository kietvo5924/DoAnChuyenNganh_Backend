package com.example.planmateapi.dto;

import com.example.planmateapi.entity.Calendar;
import com.example.planmateapi.entity.PermissionLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedCalendarResponseDto {

    private Long id;
    private String name;
    private String description;
    private boolean isDefault;
    private PermissionLevel permissionLevel;

    public static SharedCalendarResponseDto from(Calendar calendar, PermissionLevel permissionLevel) {
        return new SharedCalendarResponseDto(
                calendar.getId(),
                calendar.getName(),
                calendar.getDescription(),
                calendar.isDefault(),
                permissionLevel
        );
    }
}