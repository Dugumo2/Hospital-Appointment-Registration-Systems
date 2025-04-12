package com.graduation.his.service.entity;


import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.graduation.his.domain.enums.PermissionStatus;
import com.graduation.his.domain.po.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {


    @Override
    public List<String> getPermissionList(Object o, String s) {
        return null;
    }

    /**
     *
     * @param loginId 用户ID
     * @param loginType 多用户验证需要，未使用
     * @return 权限角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roleList = new ArrayList<>();

        int role = Db.lambdaQuery(User.class)
                .eq(User::getId, loginId)
                .one()
                .getRole();
        if(role == PermissionStatus.PATIENT.getValue()) {
            roleList.add(PermissionStatus.PATIENT.getDesc());
        }else if(role == PermissionStatus.DOCTOR.getValue()) {
            roleList.add(PermissionStatus.DOCTOR.getDesc());
        }else if(role == PermissionStatus.ADMIN.getValue()) {
            roleList.add(PermissionStatus.ADMIN.getDesc());
        }
        return roleList;
    }
}
