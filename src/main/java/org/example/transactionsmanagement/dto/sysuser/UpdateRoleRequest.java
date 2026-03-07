package org.example.transactionsmanagement.dto.sysuser;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    private String username;
    private String newRole;
}
