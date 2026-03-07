package org.example.transactionsmanagement.repository;

import org.example.transactionsmanagement.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, String> {
    //Find Username (for login)
    Optional<SysUser> findByUsername(String username);
    Optional<SysUser> findByEmail(String email);

    //Check Username and Email if any one of them are already exist in DB (for register)
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

}
