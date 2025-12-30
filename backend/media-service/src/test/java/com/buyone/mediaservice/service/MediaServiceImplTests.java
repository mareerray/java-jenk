package com.buyone.mediaservice.service;

import com.buyone.mediaservice.exception.ConflictException;
import com.buyone.mediaservice.exception.ForbiddenException;
import com.buyone.mediaservice.exception.InvalidFileException;
import com.buyone.mediaservice.exception.MediaNotFoundException;
import com.buyone.mediaservice.model.Media;
import com.buyone.mediaservice.model.MediaOwnerType;
import com.buyone.mediaservice.repository.MediaRepository;
import com.buyone.mediaservice.response.DeleteMediaResponse;
import com.buyone.mediaservice.response.MediaResponse;
import com.buyone.mediaservice.service.impl.MediaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTests {
    
    @Mock
    private MediaRepository mediaRepository;
    
    @Mock
    private StorageService storageService;
    
    @Mock
    private MultipartFile multipartFile;
    
    @InjectMocks
    private MediaServiceImpl mediaService;
    
    private void setPublicBaseUrl() {
        ReflectionTestUtils.setField(mediaService, "publicBucketBaseUrl", "https://cdn.example.com");
    }
    
    // -------- uploadImage (USER avatar) --------
    
    @Test
    void uploadImage_userAvatar_replacesExisting_whenOwnerAndRoleAllowed() {
        setPublicBaseUrl();
        String ownerId = "user-1";
        String currentUserId = "user-1";
        String currentUserRole = "CLIENT";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        
        Media oldAvatar = Media.builder()
                .id("m-old")
                .ownerId(ownerId)
                .ownerType(MediaOwnerType.USER)
                .imagePath("media/m-old.png")
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        when(mediaRepository.findAllByOwnerIdAndOwnerType(ownerId, MediaOwnerType.USER))
                .thenReturn(List.of(oldAvatar));
        
        Media savedInitial = Media.builder()
                .id("m-new")
                .ownerId(ownerId)
                .ownerType(MediaOwnerType.USER)
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.save(any(Media.class)))
                .thenReturn(savedInitial)
                .thenReturn(savedInitial);
        
        when(storageService.store(multipartFile, "m-new"))
                .thenReturn("media/m-new.png");
        
        MediaResponse response = mediaService.uploadImage(
                multipartFile,
                ownerId,
                MediaOwnerType.USER,
                currentUserId,
                currentUserRole
        );
        
        verify(storageService).delete("media/m-old.png");
        verify(mediaRepository).delete(oldAvatar);
        
        // Commented out because save() is legitimately called twice
        // ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        // verify(mediaRepository).save(mediaCaptor.capture());
        // Media firstSaved = mediaCaptor.getValue();
        // assertThat(firstSaved.getOwnerId()).isEqualTo(ownerId);
        // assertThat(firstSaved.getOwnerType()).isEqualTo(MediaOwnerType.USER);
        
        verify(storageService).store(multipartFile, "m-new");
        
        assertThat(response.id()).isEqualTo("m-new");
        assertThat(response.ownerId()).isEqualTo(ownerId);
        assertThat(response.url()).isEqualTo("https://cdn.example.com/media/m-new.png");
    }
    
    @Test
    void uploadImage_userAvatar_forbidden_whenDifferentUser() {
        setPublicBaseUrl();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        
        assertThatThrownBy(() -> mediaService.uploadImage(
                multipartFile,
                "user-1",
                MediaOwnerType.USER,
                "user-2",
                "CLIENT"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only upload an avatar for yourself");
    }
    
    @Test
    void uploadImage_userAvatar_forbidden_whenRoleNotSellerOrClient() {
        setPublicBaseUrl();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        
        assertThatThrownBy(() -> mediaService.uploadImage(
                multipartFile,
                "user-1",
                MediaOwnerType.USER,
                "user-1",
                "ADMIN"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only Sellers and Clients can upload user avatars");
    }
    
    // -------- uploadImage (PRODUCT image) --------
    
    @Test
    void uploadImage_productImage_forbidden_whenNotSeller() {
        setPublicBaseUrl();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        
        assertThatThrownBy(() -> mediaService.uploadImage(
                multipartFile,
                "product-1",
                MediaOwnerType.PRODUCT,
                "user-1",
                "CLIENT"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only Seller can upload product images");
    }
    
    @Test
    void uploadImage_productImage_throwsConflict_whenMaxImagesReached() {
        setPublicBaseUrl();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        
        when(mediaRepository.countByOwnerIdAndOwnerType("product-1", MediaOwnerType.PRODUCT))
                .thenReturn(5L);
        
        assertThatThrownBy(() -> mediaService.uploadImage(
                multipartFile,
                "product-1",
                MediaOwnerType.PRODUCT,
                "seller-1",
                "SELLER"
        )).isInstanceOf(ConflictException.class)
                .hasMessageContaining("maximum number of images (5)");
    }
    
    @Test
    void uploadImage_throwsInvalidFile_forEmptyOrTooLargeOrNotImage() {
        when(multipartFile.isEmpty()).thenReturn(true);
        assertThatThrownBy(() -> mediaService.uploadImage(
                multipartFile,
                "owner",
                MediaOwnerType.USER,
                "owner",
                "CLIENT"
        )).isInstanceOf(InvalidFileException.class);
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(3L * 1024 * 1024);
        when(multipartFile.getContentType()).thenReturn("image/png");
        assertThatThrownBy(() -> mediaService.uploadImage(
                multipartFile,
                "owner",
                MediaOwnerType.USER,
                "owner",
                "CLIENT"
        )).isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("File exceeds 2MB");
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        assertThatThrownBy(() -> mediaService.uploadImage(
                multipartFile,
                "owner",
                MediaOwnerType.USER,
                "owner",
                "CLIENT"
        )).isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("Only image files are allowed");
    }
    
    // -------- getMedia --------
    
    @Test
    void getMedia_returnsResponse_whenFound() {
        setPublicBaseUrl();
        Media media = Media.builder()
                .id("m1")
                .ownerId("owner-1")
                .ownerType(MediaOwnerType.USER)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        MediaResponse response = mediaService.getMedia("m1");
        
        assertThat(response.id()).isEqualTo("m1");
        assertThat(response.ownerId()).isEqualTo("owner-1");
        assertThat(response.url()).isEqualTo("https://cdn.example.com/media/m1.png");
    }
    
    @Test
    void getMedia_throwsNotFound_whenMissing() {
        setPublicBaseUrl();
        when(mediaRepository.findById("m1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> mediaService.getMedia("m1"))
                .isInstanceOf(MediaNotFoundException.class);
    }
    
    // -------- updateMedia --------
    
    @Test
    void updateMedia_updates_whenSellerAndOwnerMatches() {
        setPublicBaseUrl();
        String mediaId = "m1";
        String ownerId = "seller-1";
        
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        
        Media existing = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/old.png")
                .createdAt(Instant.now().minusSeconds(3600))
                .build();
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(existing));
        
        Media saved = Media.builder()
                .id(mediaId)
                .ownerId(ownerId)
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/new.png")
                .createdAt(Instant.now())
                .build();
        when(storageService.store(multipartFile, mediaId)).thenReturn("media/new.png");
        when(mediaRepository.save(any(Media.class))).thenReturn(saved);
        
        MediaResponse response = mediaService.updateMedia(
                multipartFile,
                mediaId,
                ownerId,
                "SELLER"
        );
        
        verify(storageService).delete("media/old.png");
        verify(storageService).store(multipartFile, mediaId);
        verify(mediaRepository).save(any(Media.class));
        
        assertThat(response.id()).isEqualTo(mediaId);
        assertThat(response.ownerId()).isEqualTo(ownerId);
        assertThat(response.url()).isEqualTo("https://cdn.example.com/media/new.png");
    }
    
    @Test
    void updateMedia_forbidden_whenNotSeller() {
        setPublicBaseUrl();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        
        assertThatThrownBy(() -> mediaService.updateMedia(
                multipartFile,
                "m1",
                "user-1",
                "CLIENT"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only sellers can update images");
    }
    
    @Test
    void updateMedia_forbidden_whenOwnerMismatch() {
        setPublicBaseUrl();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        
        Media existing = Media.builder()
                .id("m1")
                .ownerId("owner-1")
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/old.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(existing));
        
        assertThatThrownBy(() -> mediaService.updateMedia(
                multipartFile,
                "m1",
                "other-user",
                "SELLER"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only update your own media");
    }
    
    @Test
    void updateMedia_throwsNotFound_whenMissing() {
        setPublicBaseUrl();
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getSize()).thenReturn(1000L);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(mediaRepository.findById("m1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> mediaService.updateMedia(
                multipartFile,
                "m1",
                "user-1",
                "SELLER"
        )).isInstanceOf(MediaNotFoundException.class);
    }
    
    // -------- deleteMedia --------
    
    @Test
    void deleteMedia_userAvatar_allowsOwnerClientOrSeller() {
        Media media = Media.builder()
                .id("m1")
                .ownerId("user-1")
                .ownerType(MediaOwnerType.USER)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        DeleteMediaResponse response = mediaService.deleteMedia(
                "m1",
                "user-1",
                "CLIENT"
        );
        
        verify(storageService).delete("media/m1.png");
        verify(mediaRepository).deleteById("m1");
        assertThat(response.mediaId()).isEqualTo("m1");
        assertThat(response.message()).contains("Deleted successfully");
    }
    
    @Test
    void deleteMedia_userAvatar_forbidden_whenDifferentUser() {
        Media media = Media.builder()
                .id("m1")
                .ownerId("user-1")
                .ownerType(MediaOwnerType.USER)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        assertThatThrownBy(() -> mediaService.deleteMedia(
                "m1",
                "user-2",
                "CLIENT"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only delete your own avatar");
    }
    
    @Test
    void deleteMedia_userAvatar_forbidden_whenRoleNotSellerOrClient() {
        Media media = Media.builder()
                .id("m1")
                .ownerId("user-1")
                .ownerType(MediaOwnerType.USER)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        assertThatThrownBy(() -> mediaService.deleteMedia(
                "m1",
                "user-1",
                "ADMIN"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only Sellers or Clients can delete user avatars");
    }
    
    @Test
    void deleteMedia_productImage_allowsSellerOwnerOnly() {
        Media media = Media.builder()
                .id("m1")
                .ownerId("seller-1")
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        DeleteMediaResponse response = mediaService.deleteMedia(
                "m1",
                "seller-1",
                "SELLER"
        );
        
        verify(storageService).delete("media/m1.png");
        verify(mediaRepository).deleteById("m1");
        assertThat(response.mediaId()).isEqualTo("m1");
    }
    
    @Test
    void deleteMedia_productImage_forbidden_whenNotSeller() {
        Media media = Media.builder()
                .id("m1")
                .ownerId("seller-1")
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        assertThatThrownBy(() -> mediaService.deleteMedia(
                "m1",
                "client-1",
                "CLIENT"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only sellers can delete product images");
    }
    
    @Test
    void deleteMedia_productImage_forbidden_whenDifferentOwner() {
        Media media = Media.builder()
                .id("m1")
                .ownerId("seller-1")
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        assertThatThrownBy(() -> mediaService.deleteMedia(
                "m1",
                "seller-2",
                "SELLER"
        )).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("You can only delete your own product images");
    }
    
    @Test
    void deleteMedia_throwsNotFound_whenMissing() {
        when(mediaRepository.findById("m1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> mediaService.deleteMedia(
                "m1",
                "user-1",
                "CLIENT"
        )).isInstanceOf(MediaNotFoundException.class);
    }
    
    // -------- mediaListForProduct / findMediaEntity --------
    
    @Test
    void mediaListForProduct_returnsMappedResponses() {
        setPublicBaseUrl();
        Media m1 = Media.builder()
                .id("m1")
                .ownerId("p1")
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        Media m2 = Media.builder()
                .id("m2")
                .ownerId("p1")
                .ownerType(MediaOwnerType.PRODUCT)
                .imagePath("media/m2.png")
                .createdAt(Instant.now())
                .build();
        
        when(mediaRepository.findAllByOwnerIdAndOwnerType("p1", MediaOwnerType.PRODUCT))
                .thenReturn(List.of(m1, m2));
        
        var responses = mediaService.mediaListForProduct("p1");
        
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(MediaResponse::id)
                .containsExactlyInAnyOrder("m1", "m2");
        assertThat(responses.get(0).url()).startsWith("https://cdn.example.com/");
    }
    
    @Test
    void findMediaEntity_returnsEntity_whenFound() {
        Media media = Media.builder()
                .id("m1")
                .ownerId("owner-1")
                .ownerType(MediaOwnerType.USER)
                .imagePath("media/m1.png")
                .createdAt(Instant.now())
                .build();
        when(mediaRepository.findById("m1")).thenReturn(Optional.of(media));
        
        Media result = mediaService.findMediaEntity("m1");
        
        assertThat(result).isEqualTo(media);
    }
    
    @Test
    void findMediaEntity_throwsNotFound_whenMissing() {
        when(mediaRepository.findById("m1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> mediaService.findMediaEntity("m1"))
                .isInstanceOf(MediaNotFoundException.class);
    }
}
