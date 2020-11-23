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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.limbo.doorkeeper.api.constants.PermissionPolicy;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author Devil
 * @date 2020/11/20 9:52 AM
 */
@Data
public class PermissionApiAddParam {

    @NotNull(message = "权限id不能为空")
    @Schema(title = "权限id", required = true)
    private Long permissionId;

    @NotNull(message = "api id不能为空")
    @Schema(title = "api id", required = true)
    private Long apiId;

    /**
     * @see PermissionPolicy
     */
    @NotBlank(message = "策略不能为空")
    @Schema(title = "策略", required = true, requiredProperties = {"allow", "refuse"})
    private String policy;
}
