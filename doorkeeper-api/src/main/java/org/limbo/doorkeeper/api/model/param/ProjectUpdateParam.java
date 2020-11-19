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

package org.limbo.doorkeeper.api.model.param;

import lombok.Data;

/**
 * @author Devil
 * @date 2020/11/19 3:29 PM
 */
@Data
public class ProjectUpdateParam {

    private Long projectId;
    /**
     * 项目名称
     */
    private String name;

    /**
     * 秘钥
     */
    private String secret;

    /**
     * 描述
     */
    private String describe;
}
