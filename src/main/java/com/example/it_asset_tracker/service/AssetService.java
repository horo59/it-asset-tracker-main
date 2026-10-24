
package com.example.it_asset_tracker.service;

import com.example.it_asset_tracker.dto.asset.AssetResponse;
import com.example.it_asset_tracker.dto.asset.CreateAssetRequest;
import com.example.it_asset_tracker.entity.AppUser;
import com.example.it_asset_tracker.entity.Asset;
import com.example.it_asset_tracker.entity.Category;
import com.example.it_asset_tracker.enums.AssetStatus;
import com.example.it_asset_tracker.enums.UserRole;
import com.example.it_asset_tracker.repository.AppUserRepository;
import com.example.it_asset_tracker.repository.AssetRepository;
import com.example.it_asset_tracker.repository.CategoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository userRepository;

    public AssetService(
            AssetRepository assetRepository,
            CategoryRepository categoryRepository,
            AppUserRepository userRepository) {

        this.assetRepository = assetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    // Get all assets, optionally filtered by status.
    public List<AssetResponse> getAllAssets(AssetStatus status) {

        List<Asset> assets = status == null
                ? assetRepository.findAll()
                : assetRepository.findByStatus(status);

        return assets.stream()
                .map(AssetResponse::from)
                .toList();
    }

    // Get one asset by ID and return its DTO.
    public AssetResponse getAssetById(Long id) {

        Asset asset = getAssetEntityById(id);

        return AssetResponse.from(asset);
    }

    // Create a new asset.
    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request) {

        String serialNumber = request.serialNumber().trim();

        if (assetRepository.existsBySerialNumber(serialNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An asset with this serial number already exists");
        }

        Category category = categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category not found"));

        Asset asset = new Asset(
                request.name().trim(),
                serialNumber,
                category);

        Asset savedAsset = assetRepository.save(asset);

        return AssetResponse.from(savedAsset);
    }

    // Find an asset entity or return 404 if it does not exist.
    public Asset getAssetEntityById(Long id) {

        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Asset not found"));
    }

    // Assign an available asset to a staff member.
    @Transactional
    public AssetResponse assignAsset(Long assetId, Long userId) {

        Asset asset = getAssetEntityById(assetId);

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only AVAILABLE assets can be assigned");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        if (user.getRole() != UserRole.STAFF) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assets can only be assigned to STAFF users");
        }

        asset.assignTo(user);

        return AssetResponse.from(assetRepository.save(asset));
    }

    // Return an assigned asset to available inventory.
    @Transactional
    public AssetResponse returnAsset(Long assetId) {

        Asset asset = getAssetEntityById(assetId);

        if (asset.getStatus() != AssetStatus.ASSIGNED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ASSIGNED assets can be returned");
        }

        asset.returnAsset();

        return AssetResponse.from(assetRepository.save(asset));
    }

    // Delete an existing asset.
    @Transactional
    public void deleteAsset(Long assetId) {

        Asset asset = getAssetEntityById(assetId);

        assetRepository.delete(asset);
    }

    // Get assets assigned to a particular username.
    public List<AssetResponse> getAssetsAssignedToUserByUsername(
            String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"));

        return assetRepository.findByAssignedTo_Id(user.getId())
                .stream()
                .map(AssetResponse::from)
                .toList();
    }
}
