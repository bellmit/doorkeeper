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

package org.limbo.doorkeeper.api.constants;

import com.baomidou.mybatisplus.core.enums.IEnum;

/**
 * 策略类型
 *
 * @author Devil
 * @date 2020/12/31 3:01 下午
 */
public enum PolicyType implements IEnum<String> {
    /**
     * 基于角色
     */
    ROLE,
    /**
     * 基于用户
     */
    USER,
    /**
     * 多个策略的组合
     */
//    COMBINE,
    /**
     * 基于时间
     */
//    TIME,
    /**
     * 基于参数
     */
//    PARAM,
    /**
     * 基于用户组
     */
    GROUP,
    ;

    @Override
    public String getValue() {
        return toString();
    }

    public static PolicyType parse(String p) {
        for (PolicyType value : values()) {
            if (value.getValue().equalsIgnoreCase(p)) {
                return value;
            }
        }
        return null;
    }
}
