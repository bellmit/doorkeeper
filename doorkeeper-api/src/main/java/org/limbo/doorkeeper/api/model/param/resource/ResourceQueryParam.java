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

package org.limbo.doorkeeper.api.model.param.resource;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.limbo.doorkeeper.api.model.Page;
import org.limbo.doorkeeper.api.model.vo.ResourceVO;

import javax.validation.constraints.NotNull;

/**
 * @author Devil
 * @date 2021/1/5 4:48 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceQueryParam extends Page<ResourceVO> {

    @NotNull(message = "域不能为空")
    @Schema(title = "域", required = true)
    private Long realmId;

    @NotNull(message = "委托方不能为空")
    @Schema(title = "委托方", required = true)
    private Long clientId;

    @Schema(title = "名称", description = "精确查询")
    private String name;

    @Schema(title = "名称", description = "模糊查询")
    private String dimName;

    @Schema(title = "是否启用")
    private Boolean isEnabled;

    @Schema(title = "uri", description = "精确查询")
    private String uri;

    @Schema(title = "uri", description = "模糊查询")
    private String dimUri;

    @Schema(title = "标签名", description = "精确查询")
    private String k;

    @Schema(title = "标签名", description = "模糊查询")
    private String dimK;

    @Schema(title = "标签值", description = "精确查询")
    private String v;

    @Schema(title = "标签值", description = "模糊查询")
    private String dimV;

}