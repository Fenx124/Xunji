package com.study.platform.auth.service;

import com.study.platform.auth.dto.LoginRequest;
import com.study.platform.auth.dto.RegisterRequest;
import com.study.platform.auth.dto.UpdatePasswordRequest;
import com.study.platform.auth.dto.UpdateProfileRequest;
import com.study.platform.auth.vo.LoginResponse;
import com.study.platform.auth.vo.UserProfileVO;

public interface AuthService {

    UserProfileVO register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout();

    UserProfileVO currentUser();

    UserProfileVO updateProfile(UpdateProfileRequest request);

    void updatePassword(UpdatePasswordRequest request);
}
