package com.batchjob.service;

import com.batchjob.entity.FileRecord;
import com.batchjob.repository.FileRecordRepository;
import com.google.api.services.drive.model.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileRecordServiceTest {

    @Mock
    private GoogleDriveService googleDriveService;

    @Mock
    private FileRecordRepository fileRecordRepository;

    @InjectMocks
    private FileRecordService fileRecordService;

    private MultipartFile mockFile;
    private File driveFile;

    @BeforeEach
    void setUp() {
        mockFile = mock(MultipartFile.class);
        driveFile = new File();
        driveFile.setId("drive-123");
        driveFile.setWebViewLink("https://drive.google.com/file/d/drive-123/view");
    }

    @Test
    void uploadFiles_successfulUpload_returnsFileRecords() throws IOException {
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(googleDriveService.uploadFile(mockFile)).thenReturn(driveFile);
        when(fileRecordRepository.save(any(FileRecord.class))).thenAnswer(invocation -> {
            FileRecord record = invocation.getArgument(0);
            record.setId(1L);
            return record;
        });

        List<FileRecord> result = fileRecordService.uploadFiles(List.of(mockFile));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("test.pdf");
        assertThat(result.get(0).getFileSize()).isEqualTo(1024L);
        assertThat(result.get(0).getGoogleDriveId()).isEqualTo("drive-123");
        verify(googleDriveService).uploadFile(mockFile);
        verify(fileRecordRepository).save(any(FileRecord.class));
    }

    @Test
    void uploadFiles_multipleFiles_uploadsAll() throws IOException {
        MultipartFile mockFile2 = mock(MultipartFile.class);
        File driveFile2 = new File();
        driveFile2.setId("drive-456");
        driveFile2.setWebViewLink("https://drive.google.com/file/d/drive-456/view");

        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("file1.pdf");
        when(mockFile.getSize()).thenReturn(1024L);
        when(mockFile.getContentType()).thenReturn("application/pdf");

        when(mockFile2.isEmpty()).thenReturn(false);
        when(mockFile2.getOriginalFilename()).thenReturn("file2.txt");
        when(mockFile2.getSize()).thenReturn(512L);
        when(mockFile2.getContentType()).thenReturn("text/plain");

        when(googleDriveService.uploadFile(mockFile)).thenReturn(driveFile);
        when(googleDriveService.uploadFile(mockFile2)).thenReturn(driveFile2);
        when(fileRecordRepository.save(any(FileRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<FileRecord> result = fileRecordService.uploadFiles(Arrays.asList(mockFile, mockFile2));

        assertThat(result).hasSize(2);
        verify(googleDriveService, times(2)).uploadFile(any(MultipartFile.class));
        verify(fileRecordRepository, times(2)).save(any(FileRecord.class));
    }

    @Test
    void uploadFiles_emptyFile_skipsFile() throws IOException {
        when(mockFile.isEmpty()).thenReturn(true);

        List<FileRecord> result = fileRecordService.uploadFiles(List.of(mockFile));

        assertThat(result).isEmpty();
        verify(googleDriveService, never()).uploadFile(any());
        verify(fileRecordRepository, never()).save(any());
    }

    @Test
    void uploadFiles_ioException_continuesWithOtherFiles() throws IOException {
        MultipartFile failingFile = mock(MultipartFile.class);
        MultipartFile successFile = mock(MultipartFile.class);

        when(failingFile.isEmpty()).thenReturn(false);
        when(failingFile.getOriginalFilename()).thenReturn("failing.pdf");
        when(googleDriveService.uploadFile(failingFile)).thenThrow(new IOException("Upload failed"));

        when(successFile.isEmpty()).thenReturn(false);
        when(successFile.getOriginalFilename()).thenReturn("success.pdf");
        when(successFile.getSize()).thenReturn(2048L);
        when(successFile.getContentType()).thenReturn("application/pdf");
        when(googleDriveService.uploadFile(successFile)).thenReturn(driveFile);
        when(fileRecordRepository.save(any(FileRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<FileRecord> result = fileRecordService.uploadFiles(Arrays.asList(failingFile, successFile));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("success.pdf");
    }

    @Test
    void getAllRecords_returnsRecordsFromRepository() {
        FileRecord record1 = FileRecord.builder()
                .id(1L)
                .fileName("file1.pdf")
                .uploadedAt(LocalDateTime.now())
                .build();
        FileRecord record2 = FileRecord.builder()
                .id(2L)
                .fileName("file2.pdf")
                .uploadedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(fileRecordRepository.findAllByOrderByUploadedAtDesc()).thenReturn(Arrays.asList(record1, record2));

        List<FileRecord> result = fileRecordService.getAllRecords();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFileName()).isEqualTo("file1.pdf");
        verify(fileRecordRepository).findAllByOrderByUploadedAtDesc();
    }

    @Test
    void getAllRecords_emptyRepository_returnsEmptyList() {
        when(fileRecordRepository.findAllByOrderByUploadedAtDesc()).thenReturn(List.of());

        List<FileRecord> result = fileRecordService.getAllRecords();

        assertThat(result).isEmpty();
    }
}
