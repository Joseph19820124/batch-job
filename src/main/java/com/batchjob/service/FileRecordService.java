package com.batchjob.service;

import com.batchjob.entity.FileRecord;
import com.batchjob.repository.FileRecordRepository;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileRecordService {

    private final GoogleDriveService googleDriveService;
    private final FileRecordRepository fileRecordRepository;

    public List<FileRecord> uploadFiles(List<MultipartFile> files) {
        List<FileRecord> uploadedRecords = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            try {
                File driveFile = googleDriveService.uploadFile(file);

                FileRecord record = FileRecord.builder()
                        .fileName(file.getOriginalFilename())
                        .fileSize(file.getSize())
                        .contentType(file.getContentType())
                        .googleDriveId(driveFile.getId())
                        .googleDriveLink(driveFile.getWebViewLink())
                        .build();

                FileRecord saved = fileRecordRepository.save(record);
                uploadedRecords.add(saved);
                log.info("Saved file record: {}", saved.getFileName());

            } catch (IOException e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            }
        }

        return uploadedRecords;
    }

    public List<FileRecord> getAllRecords() {
        return fileRecordRepository.findAllByOrderByUploadedAtDesc();
    }
}
