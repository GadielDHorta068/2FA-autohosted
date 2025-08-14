package com.argy.twofactorauth.entity;

import javax.persistence.*;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String secret;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<RecoveryCode> recoveryCodes;
}