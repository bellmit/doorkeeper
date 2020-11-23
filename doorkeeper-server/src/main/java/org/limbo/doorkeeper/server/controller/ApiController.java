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

package org.limbo.doorkeeper.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.limbo.doorkeeper.api.model.Response;
import org.limbo.doorkeeper.api.model.param.ApiAddParam;
import org.limbo.doorkeeper.api.model.param.ApiUpdateParam;
import org.limbo.doorkeeper.server.entity.Api;
import org.limbo.doorkeeper.server.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Devil
 * @date 2020/11/20 10:41 AM
 */
@Tag(name = "api")
@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ApiService apiService;

    @PostMapping
    @Operation(summary = "新增api")
    public Response<Api> add(@RequestBody ApiAddParam param) {
        return Response.ok(apiService.addApi(param));
    }

    @PutMapping("/{apiId}")
    @Operation(summary = "修改api")
    public Response<Integer> update(@Validated @NotNull(message = "api不存在") @PathVariable("apiId") Long apiId,
                                    @RequestBody ApiUpdateParam param) {
        param.setApiId(apiId);
        return Response.ok(apiService.updateApi(param));
    }

    @DeleteMapping
    @Operation(summary = "批量删除api")
    public Response<Boolean> delete(@RequestBody List<Long> apiIds) {
        apiService.deleteApi(apiIds);
        return Response.ok(true);
    }

    @GetMapping
    @Operation(summary = "api列表")
    public Response<List<Api>> list() {
        return Response.ok(apiService.all());
    }

}
