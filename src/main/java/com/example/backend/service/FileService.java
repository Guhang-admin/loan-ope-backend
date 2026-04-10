package com.example.backend.service;

import com.example.backend.entity.File;
import com.example.backend.mapper.FileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

@Service
public class FileService {
    @Autowired
    private FileMapper fileMapper;

    public int getTotalFiles() {
        return fileMapper.getTotalFiles();
    }

    public int getTodayUploads() {
        return fileMapper.getTodayUploads();
    }

    public File uploadFile(MultipartFile file, String uploader) throws IOException {
        File newFile = new File();
        newFile.setFileName(file.getOriginalFilename());
        newFile.setFilePath("uploads/" + file.getOriginalFilename());
        newFile.setFileSize(file.getSize());
        newFile.setFileType(file.getContentType());
        newFile.setUploader(uploader);
        newFile.setUploadTime(new Date());
        fileMapper.insertFile(newFile);
        return newFile;
    }
}