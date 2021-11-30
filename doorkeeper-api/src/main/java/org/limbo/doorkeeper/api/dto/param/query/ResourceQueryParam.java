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
import lombok.EqualsAndHashCode;
import org.limbo.doorkeeper.api.dto.vo.ResourceVO;

import java.util.List;

/**
 * @author Devil
 * @since 2021/1/5 4:48 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceQueryParam extends PageParam<ResourceVO> {

    private Long realmId;

    private Long clientId;

    @Parameter(description ="资源ID")
    private List<Long> resourceIds;

    @Parameter(description = "名称列表，精确查询")
    private List<String> names;

    @Parameter(description = "名称，模糊查询")
    private String dimName;

    @Parameter(description ="是否启用")
    private Boolean isEnabled;

    @Parameter(description = "uri列表，精确查询")
    private List<String> uris;

    @Parameter(description = "uri，模糊查询")
    private String dimUri;

    @Parameter(description = "k=v形式，精确查询 同时满足才返回")
    private List<String> andKvs;

    @Parameter(description = "k=v形式，精确查询 满足其中一个就返回")
    private List<String> orKvs;

    @Parameter(description = "标签名，精确查询")
    private String k;

    @Parameter(description = "标签名，模糊查询")
    private String dimK;

    @Parameter(description = "标签值，精确查询")
    private String v;

    @Parameter(description = "标签值，模糊查询")
    private String dimV;

    @Parameter(description = "是否返回标签")
    private Boolean needTag;

    @Parameter(description = "是否返回uri")
    private Boolean needUri;

    @Parameter(description = "是否返回父资源ID")
    private Boolean needParentId;

    @Parameter(description = "是否返回子资源ID")
    private Boolean needChildrenId;

}
