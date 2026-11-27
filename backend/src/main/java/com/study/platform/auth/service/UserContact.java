package com.study.platform.auth.service;

/**
 * 日程提醒需要的联系方式。不带密码和角色。
 */
public record UserContact(Long id, String nickname, String email) {
}
