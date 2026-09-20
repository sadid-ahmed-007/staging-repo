package com.eduauth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.storage.dir:storage}")
    private String storageDir;

    /**
     * Saves an uploaded file to the specified sub-directory.
     *
     * @param file        The multipart file to save.
     * @param subDir      The sub-directory (e.g., "avatars", "documents").
     * @param userId      The ID of the user uploading the file (for naming).
     * @param allowEmpty  If true, won't throw exception on empty file.
     * @return The relative path to access the file (e.g., "/uploads/avatars/filename.ext").
     */
    public String storeFile(MultipartFile file, String subDir, Long userId, boolean allowEmpty) {
        if (file == null || file.isEmpty()) {
            if (allowEmpty) return null;
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        try {
            // Normalize file name
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                extension = originalFilename.substring(i);
            }

            // Create unique filename: {userId}_{timestamp}_{uuid}.ext
            String newFilename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            
            // Resolve path: <storageDir>/<subDir>
            Path uploadPath = Paths.get(storageDir, subDir).toAbsolutePath().normalize();
            
            // Create directories if they don't exist
            Files.createDirectories(uploadPath);

            Path targetLocation = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return the path relative to the web root mapped via WebMvcConfigurer
            return "/uploads/" + subDir + "/" + newFilename;
            
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    /**
     * Deletes a file given its relative URL path (e.g., "/uploads/avatars/filename.ext").
     * 
     * @param fileUrl The URL path of the file to delete.
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty() || !fileUrl.startsWith("/uploads/")) {
            return;
        }

        try {
            // Convert "/uploads/avatars/file.ext" -> "storage/avatars/file.ext"
            String relativePath = fileUrl.substring("/uploads/".length());
            Path targetLocation = Paths.get(storageDir).resolve(relativePath).toAbsolutePath().normalize();
            
            Files.deleteIfExists(targetLocation);
        } catch (IOException ex) {
            System.err.println("Could not delete file: " + fileUrl + " - " + ex.getMessage());
        }
    }
}
