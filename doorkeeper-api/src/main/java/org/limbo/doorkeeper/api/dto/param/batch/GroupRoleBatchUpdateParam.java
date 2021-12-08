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

package org.limbo.doorkeeper.api.dto.param.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.limbo.doorkeeper.api.constants.BatchMethod;
import org.limbo.doorkeeper.api.dto.param.add.GroupRoleAddParam;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Devil
 * @since 2021/1/5 11:16 上午
 */
@Data
public class GroupRoleBatchUpdateParam {

    @NotNull(message = "操作类型不能为空")
    @Schema(description ="操作类型", required = true)
    private BatchMethod type;

    @Schema(description = "角色ID列表，删除操作的时候使用")
    private List<Long> roleIds;

    @Schema(description = "角色列表，新增、更新操作的时候使用")
    private List<GroupRoleAddParam> roles;

}