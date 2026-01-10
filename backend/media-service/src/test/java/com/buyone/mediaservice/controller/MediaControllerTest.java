package com.buyone.mediaservice.controller;

import com.buyone.mediaservice.response.DeleteMediaResponse;
import com.buyone.mediaservice.response.MediaResponse;
import com.buyone.mediaservice.service.MediaService;
import com.buyone.mediaservice.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MediaController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
class MediaControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MediaService mediaService;
    
    @MockBean
    private StorageService storageService;
    
    @Test
    void listMediaForProduct_returnsOk() throws Exception {
        MediaResponse m1 = new MediaResponse("m1", "p1", "url1", Instant.now());
        MediaResponse m2 = new MediaResponse("m2", "p1", "url2", Instant.now());
        when(mediaService.mediaListForProduct("p1"))
                .thenReturn(List.of(m1, m2));
        
        mockMvc.perform(get("/media/images/product/{productId}", "p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.images.length()").value(2));
    }
    
    @Test
    void getMedia_returnsOk() throws Exception {
        MediaResponse media = new MediaResponse("m1", "owner-1", "http://url", Instant.now());
        when(mediaService.getMedia("m1")).thenReturn(media);
        
        mockMvc.perform(get("/media/images/{mediaId}", "m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("m1"));
    }
    
    @Test
    void uploadImage_returnsCreated() throws Exception {
        MediaResponse media = new MediaResponse("m1", "owner-1", "http://url", Instant.now());
        when(mediaService.uploadImage(any(), any(), any(), any(), any()))
                .thenReturn(media);
        
        mockMvc.perform(multipart("/media/images")
                        .file("file", "fake".getBytes())
                        .param("ownerId", "owner-1")
                        .param("ownerType", "USER")
                        .header("X-USER-ID", "owner-1")
                        .header("X-USER-ROLE", "CLIENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("m1"));
    }
    
    @Test
    void updateMedia_returnsOk() throws Exception {
        MediaResponse media = new MediaResponse("m1", "owner-1", "http://url", Instant.now());
        when(mediaService.updateMedia(any(), any(), any(), any()))
                .thenReturn(media);
        
        mockMvc.perform(multipart("/media/images/{mediaId}", "m1")
                        .file("file", "fake".getBytes())
                        .with(req -> { req.setMethod("PUT"); return req; })
                        .header("X-USER-ID", "owner-1")
                        .header("X-USER-ROLE", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("m1"));
    }
    
    @Test
    void deleteMedia_returnsOk() throws Exception {
        DeleteMediaResponse resp = new DeleteMediaResponse("m1", "Deleted successfully");
        when(mediaService.deleteMedia("m1", "user-1", "CLIENT")).thenReturn(resp);
        
        mockMvc.perform(delete("/media/images/{mediaId}", "m1")
                        .header("X-USER-ID", "user-1")
                        .header("X-USER-ROLE", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mediaId").value("m1"));
    }
}
