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

package org.limbo.doorkeeper.server.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.limbo.doorkeeper.server.infrastructure.po.NamespacePO;

/**
 * @author Devil
 * @since 2021/1/3 6:08 下午
 */
public interface NamespaceMapper extends BaseMapper<NamespacePO> {

    @Select("select * from doorkeeper_namespace where tenant_id = #{tenantId} and namespace_id =#{namespaceId}")
    NamespacePO getById(@Param("tenantId") Long tenantId, @Param("namespaceId") Long namespaceId);

    @Select("select * from doorkeeper_namespace where tenant_id = #{tenantId} and name =#{name}")
    NamespacePO getByName(@Param("tenantId") Long tenantId, @Param("name") String name);

}