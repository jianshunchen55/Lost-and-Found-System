package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.entity.ChatMessage;
import com.campus.lostfound.entity.Friendship;
import com.campus.lostfound.mapper.ChatMessageMapper;
import com.campus.lostfound.mapper.FriendshipMapper;
import com.campus.lostfound.repo.UserRepository;
import com.campus.lostfound.socket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatMessageService {
    private final ChatMessageMapper chatMessageMapper;
    private final UserRepository userRepository;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final FriendshipMapper friendshipMapper;

    public ChatMessageService(ChatMessageMapper chatMessageMapper, UserRepository userRepository, ChatWebSocketHandler chatWebSocketHandler, FriendshipMapper friendshipMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.userRepository = userRepository;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.friendshipMapper = friendshipMapper;
    }

    public void deleteMessage(Long id) {
        chatMessageMapper.deleteById(id);
    }

    public void deleteAll(Long userId) {
        chatMessageMapper.delete(new QueryWrapper<ChatMessage>()
            .eq("receiver_id", userId));
    }

    public Long getUnreadCount(Long userId) {
        // Get friend IDs to exclude their messages
        List<Friendship> friendships = friendshipMapper.selectList(new QueryWrapper<Friendship>()
            .eq("status", "ACCEPTED")
            .and(w -> w.eq("requester_id", userId).or().eq("addressee_id", userId)));
        
        List<Long> friendIds = new java.util.ArrayList<>();
        for (Friendship f : friendships) {
            if (f.getRequesterId().equals(userId)) {
                friendIds.add(f.getAddresseeId());
            } else {
                friendIds.add(f.getRequesterId());
            }
        }

        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<ChatMessage>()
            .eq("receiver_id", userId)
            .eq("is_read", false);

        if (!friendIds.isEmpty()) {
            wrapper.notIn("sender_id", friendIds);
        }

        return chatMessageMapper.selectCount(wrapper);
    }

    public void markAsRead(Long messageId) {
        ChatMessage msg = chatMessageMapper.selectById(messageId);
        if (msg != null) {
            msg.setIsRead(true);
            chatMessageMapper.updateById(msg);
        }
    }

    public void markAsReadFrom(Long receiverId, Long senderId) {
        ChatMessage msg = new ChatMessage();
        msg.setIsRead(true);
        chatMessageMapper.update(msg, new QueryWrapper<ChatMessage>()
            .eq("receiver_id", receiverId)
            .eq("sender_id", senderId)
            .eq("is_read", false));
    }

    public void markAllAsRead(Long userId) {
        ChatMessage msg = new ChatMessage();
        msg.setIsRead(true);
        chatMessageMapper.update(msg, new QueryWrapper<ChatMessage>()
            .eq("receiver_id", userId)
            .eq("is_read", false));
    }

    private String getDisplayName(Long viewerId, Long targetUserId) {
        return userRepository.findById(targetUserId).map(u -> {
            String name = u.getNickname() != null ? u.getNickname() : u.getUsername();
            Friendship f = friendshipMapper.selectOne(new QueryWrapper<Friendship>()
                .eq("requester_id", viewerId).eq("addressee_id", targetUserId)
                .or()
                .eq("requester_id", targetUserId).eq("addressee_id", viewerId));
            
            if (f != null && "ACCEPTED".equals(f.getStatus())) {
                if (f.getRequesterId().equals(viewerId)) {
                    if (f.getRequesterAlias() != null && !f.getRequesterAlias().isEmpty()) name = f.getRequesterAlias();
                } else {
                    if (f.getAddresseeAlias() != null && !f.getAddresseeAlias().isEmpty()) name = f.getAddresseeAlias();
                }
            }
            return name;
        }).orElse("Unknown");
    }

    public void sendMessage(Long senderId, Long receiverId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(content);
        msg.setIsRead(false);
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);

        // Notify via WebSocket
        userRepository.findById(receiverId).ifPresent(receiver -> {
            userRepository.findById(senderId).ifPresent(sender -> {
                msg.setSenderNickname(getDisplayName(receiverId, senderId));
                msg.setSenderAvatar(sender.getAvatar());
                chatWebSocketHandler.sendMessageToUser(receiver.getUsername(), msg);
            });
        });
    }

    public List<ChatMessage> getHistory(Long userId1, Long userId2) {
        return chatMessageMapper.selectList(new QueryWrapper<ChatMessage>()
            .and(w -> w.eq("sender_id", userId1).eq("receiver_id", userId2))
            .or(w -> w.eq("sender_id", userId2).eq("receiver_id", userId1))
            .orderByAsc("created_at"));
    }

    public Page<ChatMessage> getReceivedMessages(Long userId, int page, int size) {
        // Get friend IDs to exclude their messages
        List<Friendship> friendships = friendshipMapper.selectList(new QueryWrapper<Friendship>()
            .eq("status", "ACCEPTED")
            .and(w -> w.eq("requester_id", userId).or().eq("addressee_id", userId)));
        
        List<Long> friendIds = new java.util.ArrayList<>();
        for (Friendship f : friendships) {
            if (f.getRequesterId().equals(userId)) {
                friendIds.add(f.getAddresseeId());
            } else {
                friendIds.add(f.getRequesterId());
            }
        }

        QueryWrapper<ChatMessage> wrapper = new QueryWrapper<ChatMessage>()
            .eq("receiver_id", userId)
            .orderByDesc("created_at");
            
        if (!friendIds.isEmpty()) {
            wrapper.notIn("sender_id", friendIds);
        }

        Page<ChatMessage> p = chatMessageMapper.selectPage(new Page<>(page + 1, size), wrapper);
        
        if (p.getRecords() != null) {
            for (ChatMessage msg : p.getRecords()) {
                msg.setSenderNickname(getDisplayName(userId, msg.getSenderId()));
                userRepository.findById(msg.getSenderId()).ifPresent(u -> {
                    msg.setSenderAvatar(u.getAvatar());
                });
            }
        }
        return p;
    }
}
