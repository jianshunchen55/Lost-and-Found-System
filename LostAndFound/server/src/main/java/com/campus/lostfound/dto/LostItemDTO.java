package com.campus.lostfound.dto;

import lombok.Data;

@Data
public class LostItemDTO {
  private Long id;
  private String title;
  private String category;
  private String description;
  private String location;
  private Double latitude;
  private Double longitude;
  private String imageUrl;
  private String thumbnailUrl;
  private String lostTime;
  private Long pointId;
  private String contact;
  private String images;
}

