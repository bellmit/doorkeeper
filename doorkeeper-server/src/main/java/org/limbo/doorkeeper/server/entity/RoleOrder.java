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

package org.limbo.doorkeeper.server.entity;

import lombok.Data;

/**
 *
 * 给账户授予角色的时用的审核工单
 *
 * @author Devil
 * @date 2020/11/19 4:12 PM
 */
@Data
public class RoleOrder {

    private Long projectId;

    /**
     * 为哪个用户申请
     */
    private Long accountId;

    /**
     * 申请哪个角色
     */
    private Long roleId;

    /**
     * 审核的用户id
     */
    private Long auditId;
}
