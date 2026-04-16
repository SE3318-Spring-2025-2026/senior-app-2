package com.seniorapp.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column; 
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_groups") // Daha standart bir isim
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false) // Boş geçilmesin ve eşsiz olsun
    private String groupName;

    @OneToOne // Cascade sildik, kullanıcılar silinmesin!
    @JoinColumn(name = "coordinator_id", referencedColumnName = "id")
    private User coordinator;

    @OneToOne
    @JoinColumn(name = "team_leader_id", referencedColumnName = "id")
    private User teamLeader;

    @OneToMany // Cascade'i buradan da kaldırdık
    @JoinColumn(name = "group_id") // Bu sayede arada gereksiz bir tablo oluşmaz
    private List<User> members;
}