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

package org.limbo.doorkeeper.server.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.lang3.StringUtils;
import org.limbo.doorkeeper.api.model.param.PermissionAddParam;
import org.limbo.doorkeeper.api.model.param.PermissionBatchUpdateParam;
import org.limbo.doorkeeper.api.model.param.PermissionUpdateParam;
import org.limbo.doorkeeper.server.dao.PermissionMapper;
import org.limbo.doorkeeper.server.entity.Permission;
import org.limbo.doorkeeper.server.service.PermissionService;
import org.limbo.doorkeeper.server.utils.EnhancedBeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Devil
 * @date 2020/11/20 9:36 AM
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    @Transactional
    public Permission addPermission(PermissionAddParam param) {
        Permission permission = EnhancedBeanUtils.createAndCopy(param, Permission.class);
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    @Transactional
    public int updatePermission(PermissionUpdateParam param) {
        return permissionMapper.update(null, Wrappers.<Permission>lambdaUpdate()
                .set(StringUtils.isNotBlank(param.getPermissionName()), Permission::getPermissionName, param.getPermissionName())
                .set(StringUtils.isNotBlank(param.getPermissionDescribe()), Permission::getPermissionDescribe, param.getPermissionDescribe())
                .eq(Permission::getPermissionId, param.getPermissionId())
        );
    }

    @Override
    public void deletePermission(List<Long> permissionIds) {
        permissionMapper.update(null, Wrappers.<Permission>lambdaUpdate()
                .set(Permission::getIsDeleted, true)
                .in(Permission::getPermissionId, permissionIds)
        );
    }

    @Override
    public void batchUpdate(PermissionBatchUpdateParam param) {
        permissionMapper.update(null, Wrappers.<Permission>lambdaUpdate()
                .set(Permission::getIsOnline, param.getIsOnline())
                .in(Permission::getPermissionId, param.getPermissionIds())
        );
    }

    @Override
    public List<Permission> all() {
        return permissionMapper.selectList(Wrappers.emptyWrapper());
    }

}
