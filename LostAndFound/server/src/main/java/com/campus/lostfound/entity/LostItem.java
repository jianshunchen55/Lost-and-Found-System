package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

@Data
@TableName("lost_item")
public class LostItem {
  @TableId(value = "id", type = IdType.AUTO)
  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  private String title;
  private String category;
  private String description;
  private String location;
  private Double latitude;
  private Double longitude;
  private String imageUrl;
  private String thumbnailUrl;
  private String status;
  @JsonSerialize(using = ToStringSerializer.class)
  @TableField("user_id")
  private Long userId;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime updatedAt;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime lostTime;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long pointId;
  private String contact;
  private String images;
  private Integer auditStatus;
  private Integer claimStatus;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long claimUserId;
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime claimTime;
  private String claimName;
  private String claimPhone;
  private String claimAuditReply;
  @TableLogic
  private Integer deleted;
  
  @TableField(exist = false)
  private Double matchScore;
  
  @TableField(exist = false)
  private String publisherNickname;
  
  @TableField(exist = false)
  private String publisherAvatar;
  
  @TableField(exist = false)
  private String claimUserNickname;
}
