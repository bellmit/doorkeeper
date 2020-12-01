/*
 * Copyright 2020-2024 Limbo Team (https://github.com/limbo-world).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.limbo.doorkeeper.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.lang3.StringUtils;
import org.limbo.doorkeeper.admin.dao.AdminMapper;
import org.limbo.doorkeeper.admin.entity.Admin;
import org.limbo.doorkeeper.admin.model.param.AdminAddParam;
import org.limbo.doorkeeper.admin.model.param.AdminQueryParam;
import org.limbo.doorkeeper.admin.model.param.AdminUpdateParam;
import org.limbo.doorkeeper.admin.model.vo.AdminVO;
import org.limbo.doorkeeper.admin.service.AdminService;
import org.limbo.doorkeeper.admin.utils.EnhancedBeanUtils;
import org.limbo.doorkeeper.admin.utils.MD5Utils;
import org.limbo.doorkeeper.admin.utils.MyBatisPlusUtils;
import org.limbo.doorkeeper.api.client.AccountClient;
import org.limbo.doorkeeper.api.model.Page;
import org.limbo.doorkeeper.api.model.Response;
import org.limbo.doorkeeper.api.model.param.AccountAddParam;
import org.limbo.doorkeeper.api.model.param.AccountQueryParam;
import org.limbo.doorkeeper.api.model.param.AccountUpdateParam;
import org.limbo.doorkeeper.api.model.vo.AccountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * @author Devil
 * @date 2020/11/26 7:54 PM
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AccountClient accountClient;

    @Override
    @Transactional
    public Response<AccountVO> add(AdminAddParam param) {
        AccountAddParam accountAddParam = EnhancedBeanUtils.createAndCopy(param, AccountAddParam.class);
        Response<AccountVO> accountRes = accountClient.add(accountAddParam);
        if (!accountRes.ok()) {
            return accountRes;
        }

        Admin admin = EnhancedBeanUtils.createAndCopy(param, Admin.class);
        admin.setAccountId(accountRes.getData().getAccountId());
        admin.setPassword(MD5Utils.md5WithSalt(admin.getPassword()));
        adminMapper.insert(admin);
        return accountRes;
    }

    @Override
    @Transactional
    public Response<Integer> update(AdminUpdateParam param) {
        AccountUpdateParam accountUpdateParam = EnhancedBeanUtils.createAndCopy(param, AccountUpdateParam.class);

        Response<Integer> update = accountClient.update(param.getAccountId(), accountUpdateParam);
        if (!update.ok()) {
            return update;
        }

        adminMapper.update(null, Wrappers.<Admin>lambdaUpdate()
                .set(StringUtils.isNotBlank(param.getNickname()), Admin::getNickname, param.getNickname())
                .eq(Admin::getAccountId, param.getAccountId())
        );

        return update;
    }

    @Override
    public Page<AdminVO> page(AdminQueryParam param) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Admin> mpage = MyBatisPlusUtils.pageOf(param);
        LambdaQueryWrapper<Admin> condition = Wrappers.<Admin>lambdaQuery()
                .like(StringUtils.isNotBlank(param.getUsername()), Admin::getUsername, param.getUsername());
        mpage = adminMapper.selectPage(mpage, condition);

        param.setTotal(mpage.getTotal());
        param.setData(EnhancedBeanUtils.createAndCopyList(mpage.getRecords(), AdminVO.class));

        // 获取账号描述
        AccountQueryParam queryParam = new AccountQueryParam();
        queryParam.setAccountIds(param.getData().stream().map(AdminVO::getAccountId).collect(Collectors.toList()));
        queryParam.setSize(param.getSize());
        Response<Page<AccountVO>> page = accountClient.page(queryParam);
        if (page.ok()) {
            for (AccountVO accountVO : page.getData().getData()) {
                for (AdminVO adminVO : param.getData()) {
                    if (accountVO.getAccountId().equals(adminVO.getAccountId())) {
                        adminVO.setAccountDescribe(accountVO.getAccountDescribe());
                        adminVO.setIsAdmin(accountVO.getIsAdmin());
                        adminVO.setIsSuperAdmin(accountVO.getIsSuperAdmin());
                    }
                }
            }
        }

        return param;
    }

}