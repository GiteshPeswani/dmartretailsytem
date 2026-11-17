package com.dmart.blockchain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    public static final String CUSTOMER      = "CUSTOMER";
    public static final String ADMIN         = "ADMIN";
    public static final String STORE_MANAGER = "STORE_MANAGER";
}
