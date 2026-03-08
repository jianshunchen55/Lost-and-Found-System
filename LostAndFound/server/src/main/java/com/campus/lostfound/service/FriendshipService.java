package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.lostfound.entity.Friendship;
import com.campus.lostfound.entity.ChatMessage;
import com.campus.lostfound.mapper.ChatMessageMapper;
import com.campus.lostfound.mapper.FriendshipMapper;
import com.campus.lostfound.mapper.MessageMapper;
import com.campus.lostfound.repo.UserRepository;
import com.campus.lostfound.domain.User;
import com.campus.lostfound.entity.Message;
import com.campus.lostfound.dto.FriendDto;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FriendshipService {
    private final FriendshipMapper friendshipMapper;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final ChatMessageMapper chatMessageMapper;

    public FriendshipService(FriendshipMapper friendshipMapper, UserRepository userRepository, MessageMapper messageMapper, ChatMessageMapper chatMessageMapper) {
        this.friendshipMapper = friendshipMapper;
        this.userRepository = userRepository;
        this.messageMapper = messageMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    public boolean sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) return false;
        
        // Check if exists
        Friendship existing = friendshipMapper.selectOne(new QueryWrapper<Friendship>()
            .eq("requester_id", requesterId).eq("addressee_id", addresseeId)
            .or()
            .eq("requester_id", addresseeId).eq("addressee_id", requesterId));
            
        Friendship f = existing;
        boolean needNotify = false;

        if (existing != null) {
            if ("ACCEPTED".equals(existing.getStatus())) {
                return false; // Already friends
            }
            
            // If REJECTED, reset to PENDING and update requester
            if ("REJECTED".equals(existing.getStatus())) {
                existing.setStatus("PENDING");
                existing.setRequesterId(requesterId);
                existing.setAddresseeId(addresseeId);
                existing.setUpdatedAt(LocalDateTime.now());
                friendshipMapper.updateById(existing);
                needNotify = true;
            } else if ("PENDING".equals(existing.getStatus())) {
                // Already pending. Check if we need to resend notification (e.g. if missed)
                needNotify = true;
            }
        } else {
            f = new Friendship();
            f.setRequesterId(requesterId);
            f.setAddresseeId(addresseeId);
            f.setStatus("PENDING");
            f.setCreatedAt(LocalDateTime.now());
            f.setUpdatedAt(LocalDateTime.now());
            friendshipMapper.insert(f);
            needNotify = true;
        }

        // Send notification message if needed
        if (needNotify) {
            // Check if there is already an unread friend request
            Long count = messageMapper.selectCount(new QueryWrapper<Message>()
                .eq("user_id", addresseeId)
                .eq("type", "FRIEND_REQUEST")
                .eq("related_id", f.getId())
                .eq("is_read", false));
            
            if (count > 0) return true; // Already notified but considered success as it is pending

            User requester = userRepository.findById(requesterId).orElse(null);
            if (requester != null) {
                Message msg = new Message();
                msg.setUserId(addresseeId);
                msg.setTitle("好友申请");
                String requesterName = requester.getNickname() != null ? requester.getNickname() : requester.getUsername();
                msg.setContent("用户 " + requesterName + " 请求添加您为好友");
                msg.setType("FRIEND_REQUEST");
                msg.setRelatedId(f.getId());
                msg.setCreatedAt(LocalDateTime.now());
                msg.setIsRead(false);
                messageMapper.insert(msg);
            }
        }
        return true;
    }

    public void respondRequest(Long id, Long userId, boolean accept, String remark) {
        Friendship f = friendshipMapper.selectById(id);
        if (f != null && f.getAddresseeId().equals(userId) && "PENDING".equals(f.getStatus())) {
            f.setStatus(accept ? "ACCEPTED" : "REJECTED");
            f.setUpdatedAt(LocalDateTime.now());
            if (accept && remark != null && !remark.isBlank()) {
                f.setAddresseeAlias(remark);
            }
            friendshipMapper.updateById(f);
        }
    }

    public String getStatus(Long userId, Long targetId) {
        Friendship f = friendshipMapper.selectOne(new QueryWrapper<Friendship>()
            .eq("requester_id", userId).eq("addressee_id", targetId)
            .or()
            .eq("requester_id", targetId).eq("addressee_id", userId));
        
        if (f == null) return "NONE";
        return f.getStatus();
    }

    public boolean updateAlias(Long userId, Long friendId, String alias) {
        Friendship f = friendshipMapper.selectOne(new QueryWrapper<Friendship>()
            .eq("requester_id", userId).eq("addressee_id", friendId)
            .or()
            .eq("requester_id", friendId).eq("addressee_id", userId));
        
        if (f == null || !"ACCEPTED".equals(f.getStatus())) {
            return false;
        }

        if (f.getRequesterId().equals(userId)) {
            f.setRequesterAlias(alias);
        } else {
            f.setAddresseeAlias(alias);
        }
        f.setUpdatedAt(LocalDateTime.now());
        friendshipMapper.updateById(f);
        return true;
    }

    public List<FriendDto> getFriends(Long userId) {
        List<Friendship> friendships = friendshipMapper.selectList(new QueryWrapper<Friendship>()
            .and(w -> w.eq("status", "ACCEPTED"))
            .and(w -> w.eq("requester_id", userId).or().eq("addressee_id", userId)));

        List<Long> friendIds = new ArrayList<>();
        for (Friendship f : friendships) {
            if (f.getRequesterId().equals(userId)) {
                friendIds.add(f.getAddresseeId());
            } else {
                friendIds.add(f.getRequesterId());
            }
        }

        if (friendIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, User> userMap = userRepository.findAllById(friendIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        List<FriendDto> result = new ArrayList<>();
        for (Friendship f : friendships) {
            Long friendId = f.getRequesterId().equals(userId) ? f.getAddresseeId() : f.getRequesterId();
            User user = userMap.get(friendId);
            if (user != null) {
                FriendDto dto = new FriendDto();
                BeanUtils.copyProperties(user, dto);
                
                String alias = null;
                if (f.getRequesterId().equals(userId)) {
                    alias = f.getRequesterAlias();
                } else {
                    alias = f.getAddresseeAlias();
                }
                dto.setRemark(alias);
                
                // Get unread count
                Long count = chatMessageMapper.selectCount(new QueryWrapper<ChatMessage>()
                    .eq("sender_id", friendId)
                    .eq("receiver_id", userId)
                    .eq("is_read", false));
                dto.setUnreadCount(count.intValue());
                
                result.add(dto);
            }
        }
        return result;
    }
}
