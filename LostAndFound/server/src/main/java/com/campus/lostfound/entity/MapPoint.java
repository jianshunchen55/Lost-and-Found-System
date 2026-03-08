package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("map_point")
public class MapPoint {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String name;
  private Double latitude;
  private Double longitude;
  private String description;
}
