package com.batchjob.controller;

import com.batchjob.entity.FileRecord;
import com.batchjob.service.FileRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class FileUploadController {

    private final FileRecordService fileRecordService;

    @GetMapping("/")
    public String index(Model model) {
        List<FileRecord> records = fileRecordService.getAllRecords();
        model.addAttribute("records", records);
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("files") List<MultipartFile> files,
                              RedirectAttributes redirectAttributes) {
        if (files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one file");
            return "redirect:/";
        }

        List<FileRecord> uploaded = fileRecordService.uploadFiles(files);
        redirectAttributes.addFlashAttribute("success",
                String.format("Successfully uploaded %d file(s)", uploaded.size()));
        redirectAttributes.addFlashAttribute("uploadedFiles", uploaded);

        return "redirect:/result";
    }

    @GetMapping("/result")
    public String result() {
        return "result";
    }
}
