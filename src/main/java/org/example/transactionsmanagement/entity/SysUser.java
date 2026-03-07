package org.example.transactionsmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name="SYS_USER")
@NoArgsConstructor
@AllArgsConstructor
public class SysUser {
    @Id
    @Column(name="username", length = 50, nullable = false)
    private String username;

    @Column(name="password", length = 256, nullable = false)
    private String password;

    @Column(name="full_name", length = 150)
    private String fullName;

    @Column(name="email", length = 150, unique = true)
    private String email;

    @Column(name="status", length = 20)
    private String status; //ACTIVE / LOCKED / DISABLED

    @Column(name="role", length = 20)
    private String role; //UPLOADER / APPROVER / ADMIN
}
