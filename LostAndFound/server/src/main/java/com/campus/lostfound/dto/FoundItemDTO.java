package com.campus.lostfound.dto;

import lombok.Data;

@Data
public class FoundItemDTO {
  private Long id;
  private String title;
  private String category;
  private String description;
  private String location;
  private Double latitude;
  private Double longitude;
  private String imageUrl;
  private String thumbnailUrl;
  private String foundTime;
  private Long pointId;
  private String contact;
  private String images;
}
