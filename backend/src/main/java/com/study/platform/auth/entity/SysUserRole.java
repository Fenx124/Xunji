package com.study.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户-角色关联，对应表 sys_user_role（复合主键 user_id + role_id）。
 * 禁止使用 selectById / deleteById：MyBatis-Plus 只会按 user_id 命中，会误伤该用户的全部角色。
 */
@TableName("sys_user_role")
public class SysUserRole {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Long roleId;
    private LocalDateTime createdAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
