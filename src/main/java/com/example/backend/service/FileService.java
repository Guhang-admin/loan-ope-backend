package com.example.backend.service;

import com.example.backend.entity.File;
import com.example.backend.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

@Service
public class FileService {
    @Autowired
    private FileRepository fileRepository;

    private static final String UPLOAD_DIR = "uploads/";

    public File uploadFile(MultipartFile file, String uploader) throws IOException {
        // 确保上传目录存在
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成唯一文件名
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        // 保存文件
        Files.copy(file.getInputStream(), filePath);

        // 创建文件实体
        File fileEntity = new File();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFilePath(filePath.toString());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(file.getContentType());
        fileEntity.setUploadTime(new Date());
        fileEntity.setUploader(uploader);

        // 保存到数据库
        return fileRepository.save(fileEntity);
    }

    public long getTotalFiles() {
        return fileRepository.count();
    }

    public long getTodayUploads() {
        // 简单实现，实际项目中应该使用日期查询
        return fileRepository.count();
    }
}