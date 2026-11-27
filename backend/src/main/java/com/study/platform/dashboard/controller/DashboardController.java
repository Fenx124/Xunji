package com.study.platform.dashboard.controller;

import com.study.platform.common.Result;
import com.study.platform.dashboard.service.DashboardService;
import com.study.platform.dashboard.vo.DashboardOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学习看板")
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "首页学习数据看板")
    @GetMapping(value = "/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<DashboardOverviewVO> overview() {
        return Result.ok(dashboardService.overview());
    }
}
