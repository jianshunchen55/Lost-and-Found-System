package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.entity.FoundItem;
import com.campus.lostfound.entity.LostItem;
import com.campus.lostfound.mapper.FoundItemMapper;
import com.campus.lostfound.mapper.LostItemMapper;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.repo.UserRepository;
import com.campus.lostfound.util.MatchUtil;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Service
public class LostFoundService {
  private final LostItemMapper lostItemMapper;
  private final FoundItemMapper foundItemMapper;
  private final MessageMapper messageMapper;
  private final UserRepository userRepository;
  @Value("${app.upload.dir:uploads}")
  private String uploadDir;

  public LostFoundService(LostItemMapper lostItemMapper, FoundItemMapper foundItemMapper, MessageMapper messageMapper, UserRepository userRepository) {
    this.lostItemMapper = lostItemMapper;
    this.foundItemMapper = foundItemMapper;
    this.messageMapper = messageMapper;
    this.userRepository = userRepository;
  }

  public LostItem addLost(LostItem item) {
    item.setStatus("PENDING");
    item.setCreatedAt(LocalDateTime.now());
    item.setUpdatedAt(LocalDateTime.now());
    lostItemMapper.insert(item);
    
    // Check for matches and notify
    try {
        checkAndNotifyMatches(item);
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    return item;
  }

  public FoundItem addFound(FoundItem item) {
    if (item.getAuditStatus() != null && item.getAuditStatus() == 1) {
        item.setStatus("APPROVED");
    } else {
        item.setStatus("PENDING");
    }
    item.setCreatedAt(LocalDateTime.now());
    item.setUpdatedAt(LocalDateTime.now());
    foundItemMapper.insert(item);
    
    // Check for matches if approved immediately
    if ("APPROVED".equals(item.getStatus())) {
        try {
            checkAndNotifyMatchesForFound(item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    return item;
  }

  public Page<LostItem> queryLost(int page, int size, String category, String keyword, String location, String status) {
    QueryWrapper<LostItem> qw = new QueryWrapper<>();
    if (category != null && !category.isEmpty()) qw.eq("category", category);
    if (keyword != null && !keyword.isEmpty()) qw.and(w -> w.like("title", keyword).or().like("description", keyword));
    if (location != null && !location.isEmpty()) qw.like("location", location);
    if (status != null && !status.isEmpty()) {
        qw.eq("status", status);
    } else {
        // Only show items that are AUDIT APPROVED (1)
        qw.eq("audit_status", 1);
        // And status should be APPROVED (Finding) or CLAIMED (Found) or RETURNED
        // But usually we want to exclude PENDING status if audit_status=1 (which shouldn't happen but for safety)
        qw.ne("status", "PENDING");
    }
    
    // Fetch all matching lost items
    List<LostItem> lostItems = lostItemMapper.selectList(qw);
    
    // Fetch all found items for matching
    List<FoundItem> foundItems = foundItemMapper.selectList(new QueryWrapper<FoundItem>().eq("audit_status", 1));
    
    // Calculate match scores
    for (LostItem lost : lostItems) {
        double maxScore = 0;
        for (FoundItem found : foundItems) {
            double score = MatchUtil.score(
                lost.getCategory(), found.getCategory(),
                toMillis(lost.getCreatedAt()), toMillis(found.getCreatedAt()),
                lost.getLatitude() != null ? lost.getLatitude() : 0,
                lost.getLongitude() != null ? lost.getLongitude() : 0,
                found.getLatitude() != null ? found.getLatitude() : 0,
                found.getLongitude() != null ? found.getLongitude() : 0,
                lost.getDescription(), found.getDescription()
            );
            if (score > maxScore) maxScore = score;
        }
        lost.setMatchScore(maxScore);
    }
    
    // Sort by Status (APPROVED < CLAIMED) then Date Desc
    lostItems.sort((a, b) -> {
        int sA = "APPROVED".equals(a.getStatus()) ? 1 : ("CLAIMED".equals(a.getStatus()) ? 2 : 3);
        int sB = "APPROVED".equals(b.getStatus()) ? 1 : ("CLAIMED".equals(b.getStatus()) ? 2 : 3);
        if (sA != sB) return Integer.compare(sA, sB);
        
        long tA = toMillis(a.getCreatedAt());
        long tB = toMillis(b.getCreatedAt());
        return Long.compare(tB, tA); // Descending
    });
    
    // Pagination
    int total = lostItems.size();
    int start = page * size;
    if (start >= total) {
        Page<LostItem> result = new Page<>(page + 1, size);
        result.setTotal(total);
        result.setRecords(Collections.emptyList());
        return result;
    }
    int end = Math.min(start + size, total);
    List<LostItem> pageList = lostItems.subList(start, end);
    
    // Populate publisher info
    for (LostItem item : pageList) {
        if (item.getUserId() != null) {
            userRepository.findById(item.getUserId()).ifPresent(u -> {
                item.setPublisherNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
                item.setPublisherAvatar(u.getAvatar());
            });
        }
    }
    
    Page<LostItem> result = new Page<>(page + 1, size);
    result.setTotal(total);
    result.setRecords(pageList);
    return result;
  }

  private long toMillis(LocalDateTime dt) {
      if (dt == null) return 0;
      return dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  public Page<LostItem> queryLostByAuditStatus(int page, int size, Integer auditStatus) {
    QueryWrapper<LostItem> qw = new QueryWrapper<>();
    if (auditStatus != null) qw.eq("audit_status", auditStatus);
    return lostItemMapper.selectPage(new Page<>(page+1, size), qw);
  }

  public Page<FoundItem> queryFoundByAuditStatus(int page, int size, Integer auditStatus) {
    QueryWrapper<FoundItem> qw = new QueryWrapper<>();
    if (auditStatus != null) qw.eq("audit_status", auditStatus);
    return foundItemMapper.selectPage(new Page<>(page+1, size), qw);
  }

    public Page<FoundItem> queryFound(int page, int size, String category, String keyword, String location, String status, Integer auditStatus, Long userId) {
    QueryWrapper<FoundItem> qw = new QueryWrapper<>();
    if (category != null && !category.isEmpty()) qw.eq("category", category);
    if (keyword != null && !keyword.isEmpty()) qw.and(w -> w.like("title", keyword).or().like("description", keyword));
    if (location != null && !location.isEmpty()) qw.like("location", location);
    if (status != null && !status.isEmpty()) qw.eq("status", status);
    if (auditStatus != null) {
        qw.eq("audit_status", auditStatus);
    } else {
        qw.eq("audit_status", 1).ne("status", "PENDING");
    }
    
    List<FoundItem> allItems = foundItemMapper.selectList(qw);
    
    // Calculate match scores against active lost items for THIS USER ONLY
    if (userId != null) {
        List<LostItem> activeLostItems = lostItemMapper.selectList(
            new QueryWrapper<LostItem>()
                .eq("audit_status", 1)
                .eq("status", "APPROVED")
                .eq("user_id", userId) // Only match against current user's lost items
        );
        
        for (FoundItem found : allItems) {
            double maxScore = 0;
            for (LostItem lost : activeLostItems) {
                int matchCount = 0;
                
                // 1. Category Match
                if (lost.getCategory() != null && found.getCategory() != null && lost.getCategory().equals(found.getCategory())) {
                    matchCount++;
                }
                
                // 2. Location Match (Simple containment check)
                if (lost.getLocation() != null && found.getLocation() != null) {
                    if (lost.getLocation().contains(found.getLocation()) || found.getLocation().contains(lost.getLocation())) {
                        matchCount++;
                    }
                }
                
                // 3. Title/Name Match (Simple containment check)
                if (lost.getTitle() != null && found.getTitle() != null) {
                    if (lost.getTitle().contains(found.getTitle()) || found.getTitle().contains(lost.getTitle())) {
                        matchCount++;
                    }
                }
                
                double score = 0.0;
                if (matchCount == 1) score = 0.333;
                else if (matchCount == 2) score = 0.666;
                else if (matchCount >= 3) score = 0.99;
                
                if (score > maxScore) maxScore = score;
            }
            found.setMatchScore(maxScore);
        }
    } else {
        // If no user logged in, or userId not provided, score is 0
        for (FoundItem found : allItems) {
            found.setMatchScore(0.0);
        }
    }

    allItems.sort((a, b) -> {
        // Sort by Match Score Descending first
        if (a.getMatchScore() != null && b.getMatchScore() != null) {
            // We want high scores first
            int cmp = Double.compare(b.getMatchScore(), a.getMatchScore());
            // Only use score if it's significant (>0)
            if (Math.abs(b.getMatchScore() - a.getMatchScore()) > 0.001) return cmp;
        } else if (a.getMatchScore() != null && a.getMatchScore() > 0) {
            return -1;
        } else if (b.getMatchScore() != null && b.getMatchScore() > 0) {
            return 1;
        }

        int pA = getFoundPriority(a);
        int pB = getFoundPriority(b);
        if (pA != pB) return Integer.compare(pA, pB);
        if (a.getCreatedAt() == null) return 1;
        if (b.getCreatedAt() == null) return -1;
        return b.getCreatedAt().compareTo(a.getCreatedAt());
    });
    
    // Pagination
    int total = allItems.size();
    int start = page * size;
    if (start >= total) {
        Page<FoundItem> result = new Page<>(page + 1, size);
        result.setTotal(total);
        result.setRecords(Collections.emptyList());
        return result;
    }
    int end = Math.min(start + size, total);
    List<FoundItem> pageList = allItems.subList(start, end);
    
    // Populate publisher info
    for (FoundItem item : pageList) {
        if (item.getUserId() != null) {
            userRepository.findById(item.getUserId()).ifPresent(u -> {
                item.setPublisherNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
                item.setPublisherAvatar(u.getAvatar());
            });
        }
    }
    
    Page<FoundItem> result = new Page<>(page + 1, size);
    result.setTotal(total);
    result.setRecords(pageList);
    return result;
  }

  private int getFoundPriority(FoundItem item) {
      if ("APPROVED".equals(item.getStatus())) {
          if (item.getClaimStatus() != null && item.getClaimStatus() == 1) return 2; 
          return 1; 
      }
      if ("CLAIMED".equals(item.getStatus())) return 3; 
      return 4;
  }

  public Page<LostItem> getUserLostItems(Long userId, int page, int size) {
    return lostItemMapper.selectPage(new Page<>(page+1, size), new QueryWrapper<LostItem>().eq("user_id", userId));
  }

  public Page<FoundItem> getUserFoundItems(Long userId, int page, int size) {
    return foundItemMapper.selectPage(new Page<>(page+1, size), new QueryWrapper<FoundItem>().eq("user_id", userId));
  }

  public Page<FoundItem> getUserClaims(Long userId, int page, int size) {
    return foundItemMapper.selectPage(new Page<>(page+1, size), new QueryWrapper<FoundItem>().eq("claim_user_id", userId));
  }

  public void unclaimFoundItem(Long itemId, Long userId) {
      FoundItem item = foundItemMapper.selectById(itemId);
      if (item != null && userId.equals(item.getClaimUserId())) {
          foundItemMapper.update(null, new UpdateWrapper<FoundItem>()
              .eq("id", itemId)
              .set("claim_status", 0)
              .set("claim_user_id", null)
              .set("claim_time", null)
              .set("updated_at", LocalDateTime.now())
              .set("status", "CLAIMED".equals(item.getStatus()) ? "APPROVED" : item.getStatus())
          );
      }
  }

  @Scheduled(cron = "0 */5 * * * ?")
  public void scheduledMatchCheck() {
      try {
          // Fetch all APPROVED (Finding) Lost Items
          List<LostItem> lostItems = lostItemMapper.selectList(new QueryWrapper<LostItem>().eq("status", "APPROVED"));
          for (LostItem lost : lostItems) {
              try {
                  checkAndNotifyMatches(lost);
              } catch (Exception e) {
                  e.printStackTrace();
              }
          }
      } catch (Exception e) {
          e.printStackTrace();
      }
  }

  private void checkAndNotifyMatches(LostItem lost) {
        // 1. Find candidate FoundItems (Approved, same category)
        QueryWrapper<FoundItem> qw = new QueryWrapper<>();
        qw.eq("audit_status", 1)
          .eq("status", "APPROVED") // Only available items
          .eq("category", lost.getCategory());
        
        List<FoundItem> candidates = foundItemMapper.selectList(qw);
        
        // 2. Check similarity
        for (FoundItem found : candidates) {
            // Check if message already exists
            Long count = messageMapper.selectCount(new QueryWrapper<Message>()
                .eq("user_id", lost.getUserId())
                .eq("type", "RECOMMENDATION")
                .eq("related_id", found.getId()));
            
            if (count > 0) continue;

            int matchCount = 1; // Category matched by query
            
            // Location Match
            if (lost.getLocation() != null && found.getLocation() != null) {
                if (lost.getLocation().contains(found.getLocation()) || found.getLocation().contains(lost.getLocation())) {
                    matchCount++;
                }
            }
            
            // Title Match
            if (lost.getTitle() != null && found.getTitle() != null) {
                if (lost.getTitle().contains(found.getTitle()) || found.getTitle().contains(lost.getTitle())) {
                    matchCount++;
                }
            }
            
            // 99% match means 3 matches
            if (matchCount >= 3) {
                Message msg = new Message();
                msg.setUserId(lost.getUserId());
                msg.setTitle("系统推荐认领");
                msg.setContent("您发布的失物【" + lost.getTitle() + "】与现有拾物【" + found.getTitle() + "】高度相似(匹配度99%)，建议您查看。");
                msg.setType("RECOMMENDATION");
                msg.setRelatedId(found.getId());
                msg.setIsRead(false);
                msg.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(msg);
            }
        }
    }

  public void deleteLost(Long id) { lostItemMapper.deleteById(id); }
  public void deleteFound(Long id) { foundItemMapper.deleteById(id); }

  public void updateLostStatus(Long id, String status) {
      LostItem item = lostItemMapper.selectById(id);
      if(item != null) { item.setStatus(status); lostItemMapper.updateById(item); }
  }
  public void updateFoundStatus(Long id, String status) {
      FoundItem item = foundItemMapper.selectById(id);
      if(item != null) { 
          String oldStatus = item.getStatus();
          item.setStatus(status); 
          foundItemMapper.updateById(item); 
      }
  }

  public LostItem getLostById(Long id) {
      LostItem item = lostItemMapper.selectById(id);
      if (item != null && item.getUserId() != null) {
          userRepository.findById(item.getUserId()).ifPresent(u -> {
              item.setPublisherNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
              item.setPublisherAvatar(u.getAvatar());
          });
      }
      return item;
  }
  public FoundItem getFoundById(Long id) {
      FoundItem item = foundItemMapper.selectById(id);
      if (item != null && item.getUserId() != null) {
          userRepository.findById(item.getUserId()).ifPresent(u -> {
              item.setPublisherNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
              item.setPublisherAvatar(u.getAvatar());
          });
      }
      return item;
  }

  public void updateLost(LostItem item) {
      item.setUpdatedAt(LocalDateTime.now());
      lostItemMapper.updateById(item);
  }

  public void updateFound(FoundItem item) {
      item.setUpdatedAt(LocalDateTime.now());
      foundItemMapper.updateById(item);
  }

  private void deleteFileByUrl(String url) {
      if (url == null || url.isEmpty()) return;
      String u = url;
      if (u.startsWith("http://") || u.startsWith("https://")) {
          int idx = u.indexOf("/static/");
          if (idx >= 0) u = u.substring(idx);
      }
      if (u.startsWith("/static/")) {
          String name = u.substring("/static/".length());
          Path p = Paths.get(uploadDir).resolve(name);
          try { Files.deleteIfExists(p); } catch (Exception ignored) {}
      }
  }

  public void deleteLostWithFiles(Long id) {
      LostItem item = lostItemMapper.selectById(id);
      if (item != null) {
          // Soft delete first
          int rows = lostItemMapper.deleteById(id);
          if (rows > 0) {
              // If successful, delete files
              if (item.getImageUrl() != null) {
                  for (String u : item.getImageUrl().split(",")) deleteFileByUrl(u.trim());
              }
              deleteFileByUrl(item.getThumbnailUrl());
              if (item.getImages() != null && !item.getImages().isEmpty()) {
                  String s = item.getImages();
                  s = s.replace("[","").replace("]","").replace("\"","");
                  for (String u : s.split(",")) deleteFileByUrl(u.trim());
              }
          }
      }
  }

  public void deleteFoundWithFiles(Long id) {
      FoundItem item = foundItemMapper.selectById(id);
      if (item != null) {
          // Soft delete first
          int rows = foundItemMapper.deleteById(id);
          if (rows > 0) {
              if (item.getImageUrl() != null) {
                  for (String u : item.getImageUrl().split(",")) deleteFileByUrl(u.trim());
              }
              deleteFileByUrl(item.getThumbnailUrl());
          }
      }
  }

  public List<LostItem> getLostClaims(Long userId) {
      return lostItemMapper.selectList(new QueryWrapper<LostItem>().eq("claim_user_id", userId));
  }

  public List<FoundItem> getFoundClaims(Long userId) {
      return foundItemMapper.selectList(new QueryWrapper<FoundItem>().eq("claim_user_id", userId));
  }

  public Page<LostItem> queryLostClaims(int page, int size, Integer status) {
      QueryWrapper<LostItem> qw = new QueryWrapper<>();
      if (status != null) {
          qw.eq("claim_status", status);
      } else {
          qw.ne("claim_status", 0);
      }
      Page<LostItem> p = lostItemMapper.selectPage(new Page<>(page+1, size), qw);
      if (p.getRecords() != null) {
          for (LostItem item : p.getRecords()) {
              if (item.getClaimUserId() != null) {
                  userRepository.findById(item.getClaimUserId()).ifPresent(u -> item.setClaimUserNickname(u.getNickname() != null ? u.getNickname() : u.getUsername()));
              }
          }
      }
      return p;
  }

  public Page<FoundItem> queryFoundClaims(int page, int size, Integer status) {
      System.out.println("DEBUG: queryFoundClaims status=" + status);
      QueryWrapper<FoundItem> qw = new QueryWrapper<>();
      if (status != null) {
          qw.eq("claim_status", status);
      } else {
          qw.ne("claim_status", 0);
      }
      Page<FoundItem> p = foundItemMapper.selectPage(new Page<>(page+1, size), qw);
      if (p.getRecords() != null) {
          for (FoundItem item : p.getRecords()) {
              if (item.getClaimUserId() != null) {
                  userRepository.findById(item.getClaimUserId()).ifPresent(u -> item.setClaimUserNickname(u.getNickname() != null ? u.getNickname() : u.getUsername()));
              }
          }
      }
      return p;
  }

    private void checkAndNotifyMatchesForFound(FoundItem found) {
        // 1. Find candidate LostItems
        QueryWrapper<LostItem> qw = new QueryWrapper<>();
        qw.eq("audit_status", 1)
          .eq("status", "APPROVED")
          .eq("category", found.getCategory());
        
        List<LostItem> candidates = lostItemMapper.selectList(qw);
        
        // 2. Check similarity
        for (LostItem lost : candidates) {
             // Check if message already exists
            Long count = messageMapper.selectCount(new QueryWrapper<Message>()
                .eq("user_id", lost.getUserId())
                .eq("type", "RECOMMENDATION")
                .eq("related_id", found.getId()));
            
            if (count > 0) continue;

            int matchCount = 1; // Category matched
            
            if (lost.getLocation() != null && found.getLocation() != null) {
                if (lost.getLocation().contains(found.getLocation()) || found.getLocation().contains(lost.getLocation())) {
                    matchCount++;
                }
            }
            
            if (lost.getTitle() != null && found.getTitle() != null) {
                if (lost.getTitle().contains(found.getTitle()) || found.getTitle().contains(lost.getTitle())) {
                    matchCount++;
                }
            }
            
            if (matchCount >= 3) {
                Message msg = new Message();
                msg.setUserId(lost.getUserId());
                msg.setTitle("系统推荐认领");
                msg.setContent("您发布的失物【" + lost.getTitle() + "】与现有拾物【" + found.getTitle() + "】高度相似(匹配度99%)，建议您查看。");
                msg.setType("RECOMMENDATION");
                msg.setRelatedId(found.getId());
                msg.setIsRead(false);
                msg.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(msg);
            }
        }
    }

    public List<FoundItem> getSmartMatches(Long userId) {
        if (userId == null) return Collections.emptyList();
        
        // 1. Get user's active lost items
        List<LostItem> myLostItems = lostItemMapper.selectList(new QueryWrapper<LostItem>()
            .eq("user_id", userId)
            .eq("audit_status", 1)
            .eq("status", "APPROVED")
        );
        
        if (myLostItems.isEmpty()) return Collections.emptyList();
        
        // 2. Get all candidate found items
        // Match queryFound logic: exclude PENDING, include CLAIMED/APPROVED
        List<FoundItem> candidates = foundItemMapper.selectList(new QueryWrapper<FoundItem>()
            .eq("audit_status", 1)
            .ne("status", "PENDING")
        );
        
        List<FoundItem> matches = new java.util.ArrayList<>();
        
        for (FoundItem found : candidates) {
            double maxScore = 0;
            for (LostItem lost : myLostItems) {
                int matchCount = 0;
                
                // 1. Category Match
                if (lost.getCategory() != null && found.getCategory() != null && lost.getCategory().equals(found.getCategory())) {
                    matchCount++;
                }
                
                // 2. Location Match (Simple containment check)
                if (lost.getLocation() != null && found.getLocation() != null) {
                    if (lost.getLocation().contains(found.getLocation()) || found.getLocation().contains(lost.getLocation())) {
                        matchCount++;
                    }
                }
                
                // 3. Title/Name Match (Simple containment check)
                if (lost.getTitle() != null && found.getTitle() != null) {
                    if (lost.getTitle().contains(found.getTitle()) || found.getTitle().contains(lost.getTitle())) {
                        matchCount++;
                    }
                }
                
                double score = 0.0;
                if (matchCount == 1) score = 0.333;
                else if (matchCount == 2) score = 0.666;
                else if (matchCount >= 3) score = 0.99;
                
                if (score > maxScore) maxScore = score;
            }
            
            // User asked for >= 67%. Since 0.666 displays as 67% in UI, we include it.
            if (maxScore >= 0.66) {
                found.setMatchScore(maxScore);
                if (found.getUserId() != null) {
                    userRepository.findById(found.getUserId()).ifPresent(u -> {
                        found.setPublisherNickname(u.getNickname() != null ? u.getNickname() : u.getUsername());
                        found.setPublisherAvatar(u.getAvatar());
                    });
                }
                matches.add(found);
            }
        }
        
        matches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        return matches;
    }

    public void auditLostClaim(Long id, Integer status, String reply) {
      LostItem item = lostItemMapper.selectById(id);
      if (item != null) {
          Long claimUserId = item.getClaimUserId();
          
          item.setClaimStatus(status);
          item.setClaimAuditReply(reply);
          
          if (status == 2) {
              item.setStatus("CLAIMED");
          } else if (status == -1) {
              // For rejection, we keep the claim record so it can be viewed in audit history
              // But we allow re-claim in applyLostClaim
          }
          lostItemMapper.updateById(item);
          
          if (claimUserId != null) {
              Message msg = new Message();
              msg.setUserId(claimUserId);
              msg.setTitle("失物认领审核通知");
              msg.setContent("您的物品 " + item.getTitle() + " 认领申请" + (status==2?"已通过":"已被驳回") + "。\n" + 
                             (status==2 ? "请于以下时间和地点领取：\n" : "驳回理由：") + reply);
              msg.setType("SYSTEM");
              msg.setIsRead(false);
              msg.setCreatedAt(LocalDateTime.now());
              messageMapper.insert(msg);
          }
      }
  }

  public void auditFoundClaim(Long id, Integer status, String reply) {
      FoundItem item = foundItemMapper.selectById(id);
      if (item != null) {
          Long claimUserId = item.getClaimUserId();
          
          item.setClaimStatus(status);
          item.setClaimAuditReply(reply);
          
          if (status == 2) {
              item.setStatus("CLAIMED");
          } else if (status == -1) {
              // For rejection, we keep the claim record so it can be viewed in audit history
          }
          foundItemMapper.updateById(item);
          
          if (claimUserId != null) {
              Message msg = new Message();
              msg.setUserId(claimUserId);
              msg.setTitle("失物认领审核通知");
              msg.setContent("您的物品 " + item.getTitle() + " 认领申请" + (status==2?"已通过":"已被驳回") + "。\n" + 
                             (status==2 ? "请于以下时间和地点领取：\n" : "驳回理由：") + reply);
              msg.setType("SYSTEM");
              msg.setIsRead(false);
              msg.setCreatedAt(LocalDateTime.now());
              messageMapper.insert(msg);
          }
      }
  }

  public void deleteLostClaim(Long id) {
      LostItem item = lostItemMapper.selectById(id);
      if (item != null) {
          item.setClaimStatus(0);
          item.setClaimUserId(null);
          item.setClaimTime(null);
          item.setClaimAuditReply(null);
          // If it was claimed, reset to APPROVED so it can be claimed again or just exist
          if ("CLAIMED".equals(item.getStatus())) {
              item.setStatus("APPROVED");
          }
          lostItemMapper.update(null, new UpdateWrapper<LostItem>()
              .eq("id", id)
              .set("claim_status", 0)
              .set("claim_user_id", null)
              .set("claim_time", null)
              .set("claim_audit_reply", null)
              .set("status", "CLAIMED".equals(item.getStatus()) ? "APPROVED" : item.getStatus())
          );
      }
  }

  public void deleteFoundClaim(Long id) {
      FoundItem item = foundItemMapper.selectById(id);
      if (item != null) {
          item.setClaimStatus(0);
          item.setClaimUserId(null);
          item.setClaimTime(null);
          item.setClaimName(null);
          item.setClaimPhone(null);
          item.setClaimAuditReply(null);
          
          if ("CLAIMED".equals(item.getStatus())) {
              item.setStatus("APPROVED");
          }
          
          foundItemMapper.update(null, new UpdateWrapper<FoundItem>()
              .eq("id", id)
              .set("claim_status", 0)
              .set("claim_user_id", null)
              .set("claim_time", null)
              .set("claim_name", null)
              .set("claim_phone", null)
              .set("claim_audit_reply", null)
              .set("status", "CLAIMED".equals(item.getStatus()) ? "APPROVED" : item.getStatus())
          );
      }
  }
}
