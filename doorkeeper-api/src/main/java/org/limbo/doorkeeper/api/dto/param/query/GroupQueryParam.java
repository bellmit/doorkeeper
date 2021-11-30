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

package org.limbo.doorkeeper.api.dto.param.query;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;

/**
 * @author Devil
 * @since 2021/3/16 10:58 上午
 */
@Data
public class GroupQueryParam {

    @Parameter(description = "返回格式，默认列表形式，tree为树形")
    private String returnType;

    @Parameter(description ="父节点ID")
    private Long parentId;

    @Parameter(description ="名称")
    private String name;

}
