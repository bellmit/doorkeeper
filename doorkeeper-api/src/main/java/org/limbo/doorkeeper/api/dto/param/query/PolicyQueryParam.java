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
import org.limbo.doorkeeper.api.constants.PolicyType;
import org.limbo.doorkeeper.api.dto.vo.policy.PolicyVO;

/**
 * @author Devil
 * @since 2021/1/6 7:53 下午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyQueryParam extends PageParam<PolicyVO> {

    @Parameter(description = "名称，精确查询")
    private String name;

    @Parameter(description = "名称，模糊查询")
    private String dimName;

    @Parameter(description ="是否启用")
    private Boolean isEnabled;

    @Parameter(description ="类型")
    private PolicyType type;

}
