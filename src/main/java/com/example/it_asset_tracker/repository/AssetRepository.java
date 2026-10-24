
package com.example.it_asset_tracker.repository;
import com.example.it_asset_tracker.entity.Asset;
import com.example.it_asset_tracker.enums.AssetStatus;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsBySerialNumber(String serialNumber);

    List<Asset> findByStatus(AssetStatus status);

    List<Asset> findByAssignedTo_Id(Long userId);
}
