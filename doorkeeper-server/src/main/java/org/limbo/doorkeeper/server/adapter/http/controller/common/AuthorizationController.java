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

package org.limbo.doorkeeper.server.adapter.http.controller.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.limbo.doorkeeper.api.dto.vo.ResponseVO;
import org.limbo.doorkeeper.api.dto.param.query.GroupCheckParam;
import org.limbo.doorkeeper.api.dto.param.query.ResourceCheckParam;
import org.limbo.doorkeeper.api.dto.param.query.RoleCheckParam;
import org.limbo.doorkeeper.api.dto.vo.GroupVO;
import org.limbo.doorkeeper.api.dto.vo.ResourceVO;
import org.limbo.doorkeeper.api.dto.vo.RoleVO;
import org.limbo.doorkeeper.server.adapter.http.controller.BaseController;
import org.limbo.doorkeeper.server.infrastructure.checker.GroupChecker;
import org.limbo.doorkeeper.server.infrastructure.checker.ResourceChecker;
import org.limbo.doorkeeper.server.infrastructure.checker.RoleChecker;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Devil
 * @since 2021/1/18 2:30 下午
 */
@Tag(name = "鉴权")
@Slf4j
@RestController
@RequestMapping("/api/doorkeeper/v1/doorkeeper/v1/authorization")
public class AuthorizationController extends BaseController {

    @Autowired
    private ResourceChecker resourceChecker;

    @Autowired
    private RoleChecker roleChecker;

    @Autowired
    private GroupChecker groupChecker;

    @Operation(summary = "检查用户是否可以访问对应的资源")
    @GetMapping("/resource")
    public ResponseVO<List<ResourceVO>> checkResource(@ParameterObject @Validated ResourceCheckParam param) {
        return ResponseVO.success(resourceChecker.check(getUser().getUserId(), param).getResources());
    }

    @Operation(summary = "检查用户拥有的角色")
    @GetMapping("/role")
    public ResponseVO<List<RoleVO>> checkRole(@ParameterObject @Validated RoleCheckParam param) {
        return ResponseVO.success(roleChecker.check(getUser().getUserId(), param).getRoles());
    }

    @Operation(summary = "检查用户所在的组")
    @GetMapping("/group")
    public ResponseVO<List<GroupVO>> checkGroup(@ParameterObject @Validated GroupCheckParam param) {
        return ResponseVO.success(groupChecker.check(getUser().getUserId(), param).getGroups());
    }

}