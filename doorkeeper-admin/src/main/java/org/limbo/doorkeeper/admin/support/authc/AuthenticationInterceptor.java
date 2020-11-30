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

package org.limbo.doorkeeper.admin.support.authc;

import lombok.extern.slf4j.Slf4j;
import org.limbo.doorkeeper.admin.config.DoorkeeperProperties;
import org.limbo.doorkeeper.admin.session.AdminSession;
import org.limbo.doorkeeper.admin.session.RedisSessionDAO;
import org.limbo.doorkeeper.admin.session.support.SessionException;
import org.limbo.doorkeeper.admin.utils.JacksonUtil;
import org.limbo.doorkeeper.api.client.AuthenticationClient;
import org.limbo.doorkeeper.api.model.Response;
import org.limbo.doorkeeper.api.model.param.ApiCheckParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Devil
 * @date 2020/11/24 19:29
 */
@Slf4j
public class AuthenticationInterceptor implements HandlerInterceptor {

    private final DoorkeeperProperties doorkeeperProperties;

    private final RedisSessionDAO redisSessionDAO;

    @Autowired
    private AuthenticationClient authenticationClient;

    public AuthenticationInterceptor(DoorkeeperProperties doorkeeperProperties, RedisSessionDAO redisSessionDAO) {
        this.doorkeeperProperties = doorkeeperProperties;
        this.redisSessionDAO = redisSessionDAO;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 判断 url 是否有对应权限
        String sessionId = request.getHeader(doorkeeperProperties.getSession().getHeaderName());
        AdminSession adminSession = redisSessionDAO.readSessionMayNull(sessionId);
        if (adminSession == null) {
            throw new SessionException("无有效会话");
        }

        ApiCheckParam param = new ApiCheckParam();
        param.setAccountId(adminSession.getAccount().getAccountId());
        param.setMethod(request.getMethod());
        param.setPath(request.getServletPath());
        Response<Boolean> apiCheck = authenticationClient.apiCheck(param);
        if (!apiCheck.ok()) {
            log.error("api check 调用失败 ", JacksonUtil.toJSONString(apiCheck));
            return false;
        }
        if (!apiCheck.getData()) {
            throw new AuthenticationException();
        }
        return apiCheck.getData();
    }
}
