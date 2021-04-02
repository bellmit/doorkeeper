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
import org.limbo.doorkeeper.api.model.Response;
import org.limbo.doorkeeper.api.model.param.check.AuthorizationCheckParam;
import org.limbo.doorkeeper.api.model.vo.ResourceVO;
import org.limbo.doorkeeper.api.model.vo.check.AuthorizationCheckResult;
import org.limbo.doorkeeper.server.controller.BaseController;
import org.limbo.doorkeeper.server.support.auth.AuthorizationChecker;
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
 * @date 2021/1/18 2:30 下午
 */
@Tag(name = "用户资源")
@Slf4j
@RestController
@RequestMapping("/api/admin/realm/{realmId}/user/{userId}")
public class AdminUserResourceController extends BaseController {

    @Autowired
    private AuthorizationChecker authorizationChecker;

    @Operation(summary = "用户是否可以访问的资源")
    @GetMapping("/resource")
    public Response<List<ResourceVO>> checkResource(@Validated @NotNull(message = "未提交用户ID") @PathVariable("userId") Long userId,
                                                    @ParameterObject @Validated AuthorizationCheckParam param) {
        param.setUserId(userId);
        AuthorizationCheckResult check = authorizationChecker.check(param);
        return Response.success(check.getResources());
    }

}
