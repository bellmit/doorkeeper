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

import lombok.extern.slf4j.Slf4j;
import org.limbo.doorkeeper.admin.entity.AdminAccountProject;
import org.limbo.doorkeeper.admin.service.AdminAccountProjectService;
import org.limbo.doorkeeper.admin.session.AdminSession;
import org.limbo.doorkeeper.admin.session.support.SessionAccount;
import org.limbo.doorkeeper.api.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author devil
 * @date 2020/3/11
 */
@Slf4j
@RestController
@RequestMapping("/session")
public class SessionController extends BaseController {

    @Autowired
    private AdminAccountProjectService adminAccountProjectService;

    @GetMapping
    public Response<AdminSession> session() {
        AdminSession session = getSession();
        session.getSecurityDigest().setPrivateKey(null);
        return Response.ok(session);
    }

    /**
     * 切换用户当前选择的项目
     */
    @PutMapping("/project/{projectId}")
    public Response<AdminSession> switchProject(@PathVariable("projectId") @Valid @NotNull(message = "项目不存在") Long projectId) {
        AdminSession session = getSession();
        SessionAccount account = session.getAccount();

        // 检测是否有项目的操作权限
        AdminAccountProject accountProject = adminAccountProjectService.getByAccountProject(account.getAccountId(), projectId);
        if (accountProject == null) {
            return Response.paramError("无权操作此项目！");
        }

        account.setCurrentProjectId(accountProject.getProjectId());
        account.setCurrentProjectName(accountProject.getProjectName());
        sessionDAO.save(session);
        return Response.ok(getSession());
    }

    @GetMapping("/logout")
    public Response<Boolean> logout() {
        AdminSession session = getSession();
        sessionDAO.destroySession(session.getSessionId());
        return Response.ok(true);
    }

}