
package com.example.it_asset_tracker.asset;
import com.example.it_asset_tracker.entity.Asset;
import com.example.it_asset_tracker.enums.AssetStatus;
import com.example.it_asset_tracker.repository.AssetRepository;
import com.example.it_asset_tracker.service.AssetService;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.it_asset_tracker.dto.asset.AssetResponse;
import com.example.it_asset_tracker.dto.asset.CreateAssetRequest;
import com.example.it_asset_tracker.entity.Category;
import com.example.it_asset_tracker.repository.CategoryRepository;
import com.example.it_asset_tracker.entity.AppUser;
import com.example.it_asset_tracker.repository.AppUserRepository;
import com.example.it_asset_tracker.enums.UserRole;

@ExtendWith(MockitoExtension.class)
class AssetServiceTests {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private AssetService assetService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category("Computer");
    }

    @Test
    void createAssetSavesNewAvailableAsset() {
        CreateAssetRequest request =
                new CreateAssetRequest("Laptop", " SN-100 ", 1L);

        when(assetRepository.existsBySerialNumber("SN-100"))
                .thenReturn(false);
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssetResponse response = assetService.createAsset(request);

        assertEquals("Laptop", response.name());
        assertEquals("SN-100", response.serialNumber());
        assertEquals(AssetStatus.AVAILABLE, response.status());

        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void createAssetRejectsDuplicateSerialNumber() {
        CreateAssetRequest request =
                new CreateAssetRequest("Laptop", "SN-100", 1L);

        when(assetRepository.existsBySerialNumber("SN-100"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assetService.createAsset(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void createAssetRejectsMissingCategory() {
        CreateAssetRequest request =
                new CreateAssetRequest("Laptop", "SN-100", 99L);

        when(assetRepository.existsBySerialNumber("SN-100"))
                .thenReturn(false);
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assetService.createAsset(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void assignAssetToUser() {
        Asset asset = new Asset("Laptop", "SN-100", category);
        AppUser user = new AppUser("staff1", "hashed-password", UserRole.STAFF);

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(asset));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssetResponse response = assetService.assignAsset(1L, 2L);

        assertEquals(AssetStatus.ASSIGNED, response.status());
        assertEquals("staff1", response.assignedToUsername());

        verify(assetRepository).save(asset);
    }

    @Test
    void assignAssetRejectsAssetUnderMaintenance() {
        Asset asset = new Asset("Laptop", "SN-100", category);
        asset.setStatus(AssetStatus.MAINTENANCE);

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(asset));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assetService.assignAsset(1L, 2L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).findById(2L);
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void assignAssetRejectsAlreadyAssignedAsset() {
        Asset asset = new Asset("Laptop", "SN-100", category);
        asset.setStatus(AssetStatus.ASSIGNED);

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(asset));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assetService.assignAsset(1L, 2L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void returnAssignedAssetMakesItAvailable() {
        Asset asset = new Asset("Laptop", "SN-100", category);
        AppUser user = new AppUser("staff1", "hashed-password", UserRole.STAFF);
        asset.assignTo(user);

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssetResponse response = assetService.returnAsset(1L);

        assertEquals(AssetStatus.AVAILABLE, response.status());
        assertNull(response.assignedToId());
        assertNull(response.assignedToUsername());
    }

    @Test
    void returnAvailableAssetIsRejected() {
        Asset asset = new Asset("Laptop", "SN-100", category);

        when(assetRepository.findById(1L))
                .thenReturn(Optional.of(asset));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assetService.returnAsset(1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void getMissingAssetReturnsNotFound() {
        when(assetRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> assetService.getAssetEntityById(99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
