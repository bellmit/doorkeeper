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

package org.limbo.doorkeeper.admin.controller;

import org.limbo.doorkeeper.admin.model.param.AccountRoleUpdateParam;
import org.limbo.doorkeeper.admin.service.AccountRoleService;
import org.limbo.doorkeeper.api.client.AccountRoleClient;
import org.limbo.doorkeeper.api.model.Response;
import org.limbo.doorkeeper.api.model.param.AccountRoleQueryParam;
import org.limbo.doorkeeper.api.model.vo.AccountRoleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Devil
 * @date 2020/11/27 10:21 AM
 */
@RestController
@RequestMapping("/account-role")
public class AccountRoleController {

    @Autowired
    private AccountRoleClient accountRoleClient;

    @Autowired
    private AccountRoleService accountRoleService;

    @GetMapping
    public Response<List<AccountRoleVO>> list(AccountRoleQueryParam param) {
        return accountRoleClient.list(param);
    }

    @PutMapping
    public Response<Boolean> batchUpdate(@RequestBody AccountRoleUpdateParam param) {
        accountRoleService.update(param);
        return Response.ok(true);
    }

}
