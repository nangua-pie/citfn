package com.forum.rbac.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single permission, used to build the admin permission matrix.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionVO {

    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;
}
