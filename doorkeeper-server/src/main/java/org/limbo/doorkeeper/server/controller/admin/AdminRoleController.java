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

package org.limbo.doorkeeper.server.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.limbo.doorkeeper.api.model.Page;
import org.limbo.doorkeeper.api.model.Response;
import org.limbo.doorkeeper.api.model.param.role.RoleAddParam;
import org.limbo.doorkeeper.api.model.param.role.RoleBatchUpdateParam;
import org.limbo.doorkeeper.api.model.param.role.RoleQueryParam;
import org.limbo.doorkeeper.api.model.param.role.RoleUpdateParam;
import org.limbo.doorkeeper.api.model.vo.RoleVO;
import org.limbo.doorkeeper.server.controller.BaseController;
import org.limbo.doorkeeper.server.service.RoleService;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

/**
 * 如果是域角色 clientId 为 0
 * @author Devil
 * @date 2021/1/4 5:19 下午
 */
@Tag(name = "角色")
@Slf4j
@RestController
@RequestMapping("/api/admin/realm/{realmId}/role")
public class AdminRoleController extends BaseController {

    @Autowired
    private RoleService roleService;

    @Operation(summary = "新建角色")
    @PostMapping
    public Response<RoleVO> add(@RequestBody @Validated RoleAddParam param) {
        return Response.success(roleService.add(getRealmId(), param));
    }

    @Operation(summary = "查询角色列表")
    @GetMapping
    public Response<Page<RoleVO>> page(@ParameterObject @Validated RoleQueryParam param) {
        return Response.success(roleService.page(getRealmId(), param));
    }

    @Operation(summary = "查询角色")
    @GetMapping("/{roleId}")
    public Response<RoleVO> get(@Validated @NotNull(message = "未提交角色ID") @PathVariable("roleId") Long roleId) {
        return Response.success(roleService.get(getRealmId(), roleId));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/{roleId}")
    public Response<RoleVO> update(@Validated @NotNull(message = "未提交角色ID") @PathVariable("roleId") Long roleId,
                                   @Validated @RequestBody RoleUpdateParam param) {
        roleService.update(getRealmId(), roleId, param);
        return Response.success();
    }

    @Operation(summary = "批量更新角色")
    @PostMapping("/batch")
    public Response<Void> batch(@RequestBody @Validated RoleBatchUpdateParam param) {
        roleService.batchUpdate(getRealmId(), param);
        return Response.success();
    }

}
