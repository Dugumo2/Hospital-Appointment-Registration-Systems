package com.graduation.his.service.entity.impl;

import com.graduation.his.domain.po.FeedbackMessage;
import com.graduation.his.mapper.FeedbackMessageMapper;
import com.graduation.his.service.entity.IFeedbackMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 诊后反馈消息表 服务实现类
 * </p>
 *
 * @author hua
 * @since 2025-03-30
 */
@Service
public class FeedbackMessageServiceImpl extends ServiceImpl<FeedbackMessageMapper, FeedbackMessage> implements IFeedbackMessageService {

}
