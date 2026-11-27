package com.study.platform.focus.service;

import com.study.platform.common.PageResult;
import com.study.platform.focus.dto.FocusRecordCreateRequest;
import com.study.platform.focus.vo.FocusRecordVO;
import com.study.platform.focus.vo.FocusTodayStatsVO;

public interface FocusService {

    FocusRecordVO create(FocusRecordCreateRequest request);

    PageResult<FocusRecordVO> page(String startDate, String endDate, long pageNum, long pageSize);

    FocusTodayStatsVO today();
}
