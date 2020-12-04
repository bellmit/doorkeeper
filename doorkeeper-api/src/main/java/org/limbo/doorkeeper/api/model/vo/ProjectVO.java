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

package org.limbo.doorkeeper.api.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author Devil
 * @date 2020/11/19 3:29 PM
 */
@Data
public class ProjectVO {


    @Schema(title = "项目id")
    private Long projectId;

    @Schema(title = "项目名称")
    private String projectName;

    @Schema(title = "描述")
    private String projectDescribe;

    @Schema(title = "是否为管理端")
    private Boolean isAdminProject;

    @Schema(title = "创建时间")
    private Date gmtCreated;

    @Schema(title = "更新时间")
    private Date gmtModified;

}
