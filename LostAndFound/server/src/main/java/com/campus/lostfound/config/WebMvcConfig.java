package com.campus.lostfound.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  @Value("${app.upload.dir:uploads}")
  private String uploadDir;
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String path = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
    registry.addResourceHandler("/static/**").addResourceLocations(path);
  }
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOriginPatterns("*")
        .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
