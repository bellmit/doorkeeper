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
import org.limbo.doorkeeper.api.model.vo.ResponseVO;
import org.limbo.doorkeeper.api.model.param.query.GroupCheckParam;
import org.limbo.doorkeeper.api.model.param.query.ResourceCheckParam;
import org.limbo.doorkeeper.api.model.param.query.RoleCheckParam;
import org.limbo.doorkeeper.api.model.vo.GroupVO;
import org.limbo.doorkeeper.api.model.vo.ResourceVO;
import org.limbo.doorkeeper.api.model.vo.RoleVO;
import org.limbo.doorkeeper.api.model.vo.check.GroupCheckResult;
import org.limbo.doorkeeper.api.model.vo.check.ResourceCheckResult;
import org.limbo.doorkeeper.api.model.vo.check.RoleCheckResult;
import org.limbo.doorkeeper.server.controller.BaseController;
import org.limbo.doorkeeper.server.infrastructure.checker.GroupChecker;
import org.limbo.doorkeeper.server.infrastructure.checker.ResourceChecker;
import org.limbo.doorkeeper.server.infrastructure.checker.RoleChecker;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Devil
 * @date 2021/1/18 2:30 ??????
 */
@Tag(name = "????????????")
@Slf4j
@RestController
@RequestMapping("/api/admin/realm/{realmId}/user/{userId}")
public class AdminUserCheckController extends BaseController {

    @Autowired
    private ResourceChecker resourceChecker;

    @Autowired
    private RoleChecker roleChecker;

    @Autowired
    private GroupChecker groupChecker;

    @Operation(summary = "?????????????????????????????????")
    @GetMapping("/check-resource")
    public ResponseVO<List<ResourceVO>> checkResource(@Validated @NotNull(message = "???????????????ID") @PathVariable("userId") Long userId,
                                                      @ParameterObject @Validated ResourceCheckParam param) {
        ResourceCheckResult check = resourceChecker.check(userId, param);
        return ResponseVO.success(check.getResources());
    }

    @Operation(summary = "???????????????????????????")
    @GetMapping("/check-role")
    public ResponseVO<List<RoleVO>> checkRole(@Validated @NotNull(message = "???????????????ID") @PathVariable("userId") Long userId,
                                              @ParameterObject @Validated RoleCheckParam param) {
        RoleCheckResult result = roleChecker.check(userId, param);
        return ResponseVO.success(result.getRoles());
    }

    @Operation(summary = "????????????????????????")
    @GetMapping("/check-group")
    public ResponseVO<List<GroupVO>> checkGroup(@Validated @NotNull(message = "???????????????ID") @PathVariable("userId") Long userId,
                                                @ParameterObject @Validated GroupCheckParam param) {
        GroupCheckResult result = groupChecker.check(userId, param);
        return ResponseVO.success(result.getGroups());
    }

}
