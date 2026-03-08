package com.campus.lostfound.dto;

import com.campus.lostfound.domain.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FriendDto extends User {
    private String remark;
    private Integer unreadCount;
}
