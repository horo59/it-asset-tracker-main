
package com.example.it_asset_tracker.controller;

import com.example.it_asset_tracker.dto.asset.AssetResponse;
import com.example.it_asset_tracker.dto.asset.CreateAssetRequest;
import com.example.it_asset_tracker.enums.AssetStatus;
import com.example.it_asset_tracker.service.AssetService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@SecurityRequirement(name = "bearerAuth")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    // Get all assets, optionally filtered by status.
    @GetMapping
    public ResponseEntity<List<AssetResponse>> getAllAssets(
            @RequestParam(required = false) AssetStatus status) {

        return ResponseEntity.ok(
                assetService.getAllAssets(status));
    }

    // Get one asset by its ID.
    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> getAssetById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                assetService.getAssetById(id));
    }

    // Create a new asset.
    @PostMapping
    public ResponseEntity<AssetResponse> createAsset(
            @Valid @RequestBody CreateAssetRequest request) {

        AssetResponse response = assetService.createAsset(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    // Get assets assigned to the authenticated user.
    @GetMapping("/my-assets")
    public ResponseEntity<List<AssetResponse>> getMyAssets(
            Authentication authentication) {

        return ResponseEntity.ok(
                assetService.getAssetsAssignedToUserByUsername(
                        authentication.getName()));
    }

    // Assign an available asset to a staff member.
    @PatchMapping("/{id}/assign/{userId}")
    public ResponseEntity<AssetResponse> assignAsset(
            @PathVariable Long id,
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                assetService.assignAsset(id, userId));
    }

    // Return an assigned asset.
    @PatchMapping("/{id}/return")
    public ResponseEntity<AssetResponse> returnAsset(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                assetService.returnAsset(id));
    }

    // Delete an asset.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable Long id) {

        assetService.deleteAsset(id);

        return ResponseEntity.noContent().build();
    }
}
