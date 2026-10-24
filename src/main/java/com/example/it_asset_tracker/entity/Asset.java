
package com.example.it_asset_tracker.entity;
import com.example.it_asset_tracker.enums.AssetStatus;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private AppUser assignedTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Asset() {
        // Required by JPA
    }

    public Asset(String name, String serialNumber, Category category) {
        this.name = name;
        this.serialNumber = serialNumber;
        this.category = category;
        this.status = AssetStatus.AVAILABLE;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public AssetStatus getStatus() {
        return status;
    }

    public Category getCategory() {
        return category;
    }

    public AppUser getAssignedTo() {
        return assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setStatus(AssetStatus status) {
        this.status = status;
    }

    public void setCategory(Category category) {
    this.category = category;
}

public void assignTo(AppUser user) {
    this.assignedTo = user;
    this.status = AssetStatus.ASSIGNED;
}

public void returnAsset() {
    this.assignedTo = null;
    this.status = AssetStatus.AVAILABLE;
}

    public void setAssignedTo(AppUser assignedTo) {
        this.assignedTo = assignedTo;
    }
}
