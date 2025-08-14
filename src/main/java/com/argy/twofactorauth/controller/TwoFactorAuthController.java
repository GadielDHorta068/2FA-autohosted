package com.argy.twofactorauth.controller;

import com.argy.twofactorauth.dto.Enable2FARequest;
import com.argy.twofactorauth.dto.Enable2FAResponse;
import com.argy.twofactorauth.dto.Verify2FARequest;
import com.argy.twofactorauth.dto.Verify2FAResponse;
import com.argy.twofactorauth.service.TwoFactorAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/2fa")
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    public TwoFactorAuthController(TwoFactorAuthService twoFactorAuthService) {
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @PostMapping("/enable")
    public Enable2FAResponse enable2FA(@RequestBody Enable2FARequest request) throws Exception {
        return twoFactorAuthService.enable2FA(request);
    }

    @PostMapping("/verify")
    public Verify2FAResponse verify2FA(@RequestBody Verify2FARequest request) throws Exception {
        return twoFactorAuthService.verify2FA(request);
    }
}   