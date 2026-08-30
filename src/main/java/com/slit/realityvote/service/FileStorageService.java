package com.slit.realityvote.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Handles saving uploaded contestant photos to disk under
 * ./uploads/contestants/ and returning the relative path to store
 * on the Contestant entity (photoPath).
 */
@Service
public class FileStorageService {

    private final Path uploadRoot = Paths.get("uploads", "contestants");
    private final Path showUploadRoot = Paths.get("uploads", "shows");
    private final Path judgeUploadRoot = Paths.get("uploads", "judges");

    public FileStorageService() {
        try {
            Files.createDirectories(uploadRoot);
            Files.createDirectories(showUploadRoot);
            Files.createDirectories(judgeUploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload directory", e);
        }
    }

    /**
     * Saves the file with a random-prefixed name (to avoid collisions /
     * guessable file paths) and returns the relative path used both to
     * store in the DB and to build the public URL: /uploads/{relativePath}
     */
    public String storeContestantPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg");
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Path target = uploadRoot.resolve(filename);
            Files.copy(file.getInputStream(), target);
            return "contestants/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store contestant photo: " + original, e);
        }
    }

    /**
     * Saves a reality show's poster/cover photo under ./uploads/shows/ and
     * returns the relative path used both to store on the entity
     * (posterImagePath) and to build the public URL: /uploads/{relativePath}
     */
    public String storeShowPoster(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "poster.jpg");
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Path target = showUploadRoot.resolve(filename);
            Files.copy(file.getInputStream(), target);
            return "shows/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store show poster: " + original, e);
        }
    }

    /**
     * Saves a judge's profile photo under ./uploads/judges/ and returns
     * the relative path used both to store on the entity (photoPath) and
     * to build the public URL: /uploads/{relativePath}
     */
    public String storeJudgePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo.jpg");
        String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String filename = UUID.randomUUID() + extension;

        try {
            Path target = judgeUploadRoot.resolve(filename);
            Files.copy(file.getInputStream(), target);
            return "judges/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store judge photo: " + original, e);
        }
    }
}