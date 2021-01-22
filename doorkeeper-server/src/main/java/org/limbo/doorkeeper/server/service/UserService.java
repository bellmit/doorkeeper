/*
 * Copyright 2020-2024 Limbo Team (https://github.com/limbo-world).
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   	http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.limbo.doorkeeper.server.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.lang3.StringUtils;
import org.limbo.doorkeeper.api.exception.ParamException;
import org.limbo.doorkeeper.api.model.Page;
import org.limbo.doorkeeper.api.model.param.user.UserAddParam;
import org.limbo.doorkeeper.api.model.param.user.UserQueryParam;
import org.limbo.doorkeeper.api.model.param.user.UserUpdateParam;
import org.limbo.doorkeeper.api.model.vo.UserVO;
import org.limbo.doorkeeper.server.dao.UserMapper;
import org.limbo.doorkeeper.server.entity.User;
import org.limbo.doorkeeper.server.utils.EnhancedBeanUtils;
import org.limbo.doorkeeper.server.utils.MD5Utils;
import org.limbo.doorkeeper.server.utils.MyBatisPlusUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Devil
 * @date 2021/1/9 7:57 下午
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public UserVO add(Long realmId, UserAddParam param) {
        User user = EnhancedBeanUtils.createAndCopy(param, User.class);
        user.setRealmId(realmId);
        user.setPassword(MD5Utils.md5WithSalt(param.getPassword()));
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new ParamException("用户已存在");
        }
        user.setPassword(null);
        return EnhancedBeanUtils.createAndCopy(user, UserVO.class);
    }

    public Page<UserVO> page(Long realmId, UserQueryParam param) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> mpage = MyBatisPlusUtils.pageOf(param);
        mpage = userMapper.selectPage(mpage, Wrappers.<User>lambdaQuery()
                .eq(User::getRealmId, realmId)
                .like(StringUtils.isNotBlank(param.getDimName()), User::getUsername, param.getDimName())
                .like(StringUtils.isNotBlank(param.getDimName()), User::getNickname, param.getDimName())
                .orderByDesc(User::getUserId)
        );

        for (User user : mpage.getRecords()) {
            user.setPassword(null);
        }

        param.setTotal(mpage.getTotal());
        param.setData(EnhancedBeanUtils.createAndCopyList(mpage.getRecords(), UserVO.class));
        return param;
    }

    public UserVO get(Long realmId, Long userId, String username) {
        if (userId == null && StringUtils.isBlank(username)) {
            throw new ParamException("未传递查询参数");
        }
        User user;
        if (userId != null) {
            user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getUserId, userId)
                    .eq(User::getRealmId, realmId)
            );
        } else {
            user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getUsername, username)
                    .eq(User::getRealmId, realmId)
            );
        }
        user.setPassword(null);
        return EnhancedBeanUtils.createAndCopy(user, UserVO.class);
    }

    @Transactional
    public void update(Long realmId, Long userId, UserUpdateParam param) {
        LambdaUpdateWrapper<User> updateWrapper = Wrappers.<User>lambdaUpdate()
                .set(StringUtils.isNotBlank(param.getNickname()), User::getNickname, param.getNickname())
                .set(StringUtils.isNotBlank(param.getNickname()), User::getNickname, param.getNickname())
                .set(param.getDescription() != null, User::getDescription, param.getDescription())
                .set(param.getIsEnabled() != null, User::getIsEnabled, param.getIsEnabled())
                .eq(User::getUserId, userId)
                .eq(User::getRealmId, realmId);
        updateWrapper.set(StringUtils.isNotEmpty(param.getPassword()), User::getPassword, param.getPassword());
        userMapper.update(null, updateWrapper);
    }

}
