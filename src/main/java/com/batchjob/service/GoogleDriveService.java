package com.batchjob.service;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;

@Service
@Slf4j
public class GoogleDriveService {

    private final Drive driveService;

    @Value("${google.drive.folder-id}")
    private String folderId;

    @Autowired
    public GoogleDriveService(@Autowired(required = false) Drive driveService) {
        this.driveService = driveService;
        if (driveService == null) {
            log.warn("Google Drive service not configured - uploads will be simulated");
        }
    }

    public boolean isConfigured() {
        return driveService != null;
    }

    public File uploadFile(MultipartFile multipartFile) throws IOException {
        if (driveService == null) {
            log.info("Simulating upload for: {}", multipartFile.getOriginalFilename());
            File mockFile = new File();
            mockFile.setId("mock-" + System.currentTimeMillis());
            mockFile.setName(multipartFile.getOriginalFilename());
            mockFile.setWebViewLink("https://drive.google.com/mock");
            mockFile.setSize(multipartFile.getSize());
            return mockFile;
        }

        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());
        fileMetadata.setParents(Collections.singletonList(folderId));

        InputStreamContent mediaContent = new InputStreamContent(
                multipartFile.getContentType(),
                multipartFile.getInputStream()
        );

        File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setSupportsAllDrives(true)
                .setFields("id, name, webViewLink, size")
                .execute();

        log.info("Uploaded file: {} with ID: {}", uploadedFile.getName(), uploadedFile.getId());
        return uploadedFile;
    }
}
