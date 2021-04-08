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

package org.limbo.doorkeeper.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.limbo.doorkeeper.api.model.Response;
import org.limbo.doorkeeper.api.model.param.check.AuthorizationCheckParam;
import org.limbo.doorkeeper.api.model.param.check.GroupCheckParam;
import org.limbo.doorkeeper.api.model.param.check.RoleCheckParam;
import org.limbo.doorkeeper.api.model.vo.GroupVO;
import org.limbo.doorkeeper.api.model.vo.ResourceVO;
import org.limbo.doorkeeper.api.model.vo.RoleVO;
import org.limbo.doorkeeper.api.model.vo.UserVO;
import org.limbo.doorkeeper.api.model.vo.check.AuthorizationCheckResult;
import org.limbo.doorkeeper.server.service.GroupUserService;
import org.limbo.doorkeeper.server.service.UserRoleService;
import org.limbo.doorkeeper.server.support.auth.AuthorizationCheckerFactory;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Devil
 * @date 2021/1/18 2:30 下午
 */
@Tag(name = "鉴权")
@Slf4j
@RestController
@RequestMapping("/api/authorization")
public class AuthorizationController extends BaseController {

    @Autowired
    private AuthorizationCheckerFactory authorizationCheckerFactory;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private GroupUserService groupUserService;

    @Operation(summary = "检查用户是否可以访问对应的资源")
    @GetMapping("/resource")
    public Response<List<ResourceVO>> checkResource(@ParameterObject @Validated AuthorizationCheckParam param) {
        param.setUserId(getUser().getUserId());
        AuthorizationCheckResult check = authorizationCheckerFactory.createChecker().check(param);
        return Response.success(check.getResources());
    }

    @Operation(summary = "检查用户拥有的角色")
    @GetMapping("/role")
    public Response<List<RoleVO>> checkRole(@ParameterObject @Validated RoleCheckParam param) {
        UserVO user = getUser();
        return Response.success(userRoleService.checkRole(user.getUserId(), user.getRealmId(), param));
    }

    @Operation(summary = "检查用户所在的组")
    @GetMapping("/group")
    public Response<List<GroupVO>> checkGroup(@ParameterObject @Validated GroupCheckParam param) {
        UserVO user = getUser();
        return Response.success(groupUserService.checkGroup(user.getUserId(), user.getRealmId(), param));
    }

}
