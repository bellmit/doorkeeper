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

package org.limbo.doorkeeper.server.config;

import org.limbo.doorkeeper.api.constants.DoorkeeperConstants;
import org.limbo.doorkeeper.server.dao.ProjectMapper;
import org.limbo.doorkeeper.server.entity.Project;
import org.limbo.doorkeeper.server.utils.Verifies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Devil
 * @date 2020/11/24 8:28 PM
 */
public class DoorkeeperInterceptor implements HandlerInterceptor {

    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 判断是否为 admin 项目
        String projectId = request.getHeader(DoorkeeperConstants.PROJECT_HEADER);
        String secret = request.getHeader(DoorkeeperConstants.PROJECT_SECRET_HEADER);

        Project project = projectMapper.selectById(Long.valueOf(projectId));
        Verifies.verify(project.getProjectSecret().equals(secret), "项目认证失败");

        Long projectParamId = project.getIsAdmin() ? Long.valueOf(request.getHeader(DoorkeeperConstants.PROJECT_PARAM_HEADER))
                : Long.valueOf(request.getHeader(DoorkeeperConstants.PROJECT_HEADER));

        request.setAttribute(DoorkeeperConstants.PROJECT_PARAM_HEADER, projectParamId);
        return true;
    }
}
