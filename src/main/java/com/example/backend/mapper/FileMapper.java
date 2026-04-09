package com.example.backend.mapper;

import com.example.backend.entity.File;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper {
    int getTotalFiles();
    int getTodayUploads();
    void insertFile(File file);
}