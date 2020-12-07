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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.limbo.doorkeeper.api.exception.ParamException;
import org.limbo.doorkeeper.api.model.Page;
import org.limbo.doorkeeper.api.model.param.RoleAddParam;
import org.limbo.doorkeeper.api.model.param.RoleQueryParam;
import org.limbo.doorkeeper.api.model.param.RoleUpdateParam;
import org.limbo.doorkeeper.api.model.vo.RoleVO;
import org.limbo.doorkeeper.server.constants.BusinessType;
import org.limbo.doorkeeper.server.constants.OperateType;
import org.limbo.doorkeeper.server.dao.AccountAdminRoleMapper;
import org.limbo.doorkeeper.server.dao.AccountRoleMapper;
import org.limbo.doorkeeper.server.dao.RoleMapper;
import org.limbo.doorkeeper.server.dao.RolePermissionMapper;
import org.limbo.doorkeeper.server.entity.AccountRole;
import org.limbo.doorkeeper.server.entity.Role;
import org.limbo.doorkeeper.server.entity.RolePermission;
import org.limbo.doorkeeper.server.service.AccountRoleService;
import org.limbo.doorkeeper.server.service.RolePermissionService;
import org.limbo.doorkeeper.server.service.RoleService;
import org.limbo.doorkeeper.server.support.plog.PLog;
import org.limbo.doorkeeper.server.support.plog.PLogConstants;
import org.limbo.doorkeeper.server.support.plog.PLogTag;
import org.limbo.doorkeeper.server.utils.EnhancedBeanUtils;
import org.limbo.doorkeeper.server.utils.MyBatisPlusUtils;
import org.limbo.doorkeeper.server.utils.Verifies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Devil
 * @date 2020/11/19 5:44 PM
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private AccountRoleMapper accountRoleMapper;

    @Autowired
    private AccountRoleService accountRoleService;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private AccountAdminRoleMapper accountAdminRoleMapper;

    @Override
    @Transactional
    @PLog(operateType = OperateType.CREATE, businessType = BusinessType.ROLE)
    public RoleVO addRole(@PLogTag(PLogConstants.CONTENT) Long projectId,
                          @PLogTag(PLogConstants.CONTENT) RoleAddParam param) {
        Role role = EnhancedBeanUtils.createAndCopy(param, Role.class);
        role.setProjectId(projectId);
        try {
            roleMapper.insert(role);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new ParamException("角色已存在");
        }
        return EnhancedBeanUtils.createAndCopy(role, RoleVO.class);
    }

    @Override
    @Transactional
    @PLog(operateType = OperateType.UPDATE, businessType = BusinessType.ROLE)
    public Integer updateRole(@PLogTag(PLogConstants.CONTENT) Long projectId,
                              @PLogTag(PLogConstants.CONTENT) RoleUpdateParam param) {
        Role role = roleMapper.selectById(param.getRoleId());
        Verifies.notNull(role, "角色不存在");
        int update;
        try {
            update = roleMapper.update(null, Wrappers.<Role>lambdaUpdate()
                    .set(StringUtils.isNotBlank(param.getRoleName()), Role::getRoleName, param.getRoleName())
                    .set(StringUtils.isNotBlank(param.getRoleDescribe()), Role::getRoleDescribe, param.getRoleDescribe())
                    .set(param.getIsDefault() != null, Role::getIsDefault, param.getIsDefault())
                    .eq(Role::getProjectId, projectId)
                    .eq(Role::getRoleId, param.getRoleId())
            );
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new ParamException("角色已存在");
        }
        return update;
    }

    @Override
    @Transactional
    @PLog(operateType = OperateType.DELETE, businessType = BusinessType.ROLE)
    public Integer deleteRole(@PLogTag(PLogConstants.CONTENT) Long projectId,
                              @PLogTag(PLogConstants.CONTENT) List<Long> roleIds) {

        Integer result = roleMapper.delete(Wrappers.<Role>lambdaQuery()
                .in(Role::getRoleId, roleIds)
                .eq(Role::getProjectId, projectId)
        );

        // 删除账户角色绑定
        List<AccountRole> accountRoles = accountRoleMapper.selectList(Wrappers.<AccountRole>lambdaQuery()
                .select(AccountRole::getAccountRoleId)
                .in(AccountRole::getRoleId, roleIds)
                .eq(AccountRole::getProjectId, projectId)
        );
        if (CollectionUtils.isNotEmpty(accountRoles)) {
            accountRoleService.batchDelete(projectId,
                    accountRoles.stream().map(AccountRole::getAccountRoleId).collect(Collectors.toList()));
        }

        // 删除角色权限绑定
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(Wrappers.<RolePermission>lambdaQuery()
                .select(RolePermission::getRolePermissionId)
                .in(RolePermission::getRoleId, roleIds)
                .eq(RolePermission::getProjectId, projectId)
        );
        if (CollectionUtils.isNotEmpty(rolePermissions)) {
            rolePermissionService.deleteRolePermission(projectId,
                    rolePermissions.stream().map(RolePermission::getRolePermissionId).collect(Collectors.toList()));
        }

        // 删除账户管理端角色绑定 todo

        return result;
    }

    @Override
    public Page<RoleVO> queryRole(Long projectId, RoleQueryParam param) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Role> mpage = MyBatisPlusUtils.pageOf(param);
        LambdaQueryWrapper<Role> condition = Wrappers.<Role>lambdaQuery()
                .like(StringUtils.isNotBlank(param.getRoleName()), Role::getRoleName, param.getRoleName())
                .eq(Role::getProjectId, projectId);
        mpage = roleMapper.selectPage(mpage, condition);

        param.setTotal(mpage.getTotal());
        param.setData(EnhancedBeanUtils.createAndCopyList(mpage.getRecords(), RoleVO.class));
        return param;
    }

    @Override
    public List<RoleVO> list(Long projectId) {
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                .eq(Role::getProjectId, projectId)
        );
        return EnhancedBeanUtils.createAndCopyList(roles, RoleVO.class);
    }

    @Override
    public List<RoleVO> adminRoles() {
        return roleMapper.adminRoles();
    }

}
