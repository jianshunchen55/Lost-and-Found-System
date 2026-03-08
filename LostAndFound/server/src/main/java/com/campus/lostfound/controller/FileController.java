package com.campus.lostfound.controller;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {
  @Value("${app.upload.dir:uploads}")
  private String uploadDir;

  @PostMapping("/upload")
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
    if (file.getSize() > 50 * 1024 * 1024) {
      return ResponseEntity.badRequest().body(Map.of("message", "文件超过50MB限制"));
    }
    String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
    String name = UUID.randomUUID().toString().replace("-", "") + "." + (ext == null ? "jpg" : ext);
    Path dir = Paths.get(uploadDir).toAbsolutePath();
    Files.createDirectories(dir);
    Path origin = dir.resolve(name);
    file.transferTo(origin.toFile());
    String thumbName = UUID.randomUUID().toString().replace("-", "") + "_thumb." + (ext == null ? "jpg" : ext);
    Path thumb = dir.resolve(thumbName);
    try {
      Thumbnails.of(origin.toFile()).size(600, 600).toFile(thumb.toFile());
    } catch (Exception e) {
      thumb = origin;
    }
    return ResponseEntity.ok(Map.of("url", "/static/" + name, "thumb", "/static/" + thumb.getFileName().toString()));
  }
}
