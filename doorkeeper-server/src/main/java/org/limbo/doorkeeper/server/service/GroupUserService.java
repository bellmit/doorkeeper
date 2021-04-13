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

package org.limbo.doorkeeper.server.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.limbo.doorkeeper.api.model.param.check.GroupCheckParam;
import org.limbo.doorkeeper.api.model.param.group.GroupUserBatchUpdateParam;
import org.limbo.doorkeeper.api.model.vo.GroupUserVO;
import org.limbo.doorkeeper.api.model.vo.GroupVO;
import org.limbo.doorkeeper.server.dal.entity.Group;
import org.limbo.doorkeeper.server.dal.entity.GroupUser;
import org.limbo.doorkeeper.server.dal.mapper.GroupMapper;
import org.limbo.doorkeeper.server.dal.mapper.GroupUserMapper;
import org.limbo.doorkeeper.server.utils.EnhancedBeanUtils;
import org.limbo.doorkeeper.server.utils.Verifies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Devil
 * @date 2021/1/12 3:31 下午
 */
@Service
public class GroupUserService {

    @Autowired
    private GroupUserMapper groupUserMapper;

    @Autowired
    private GroupMapper groupMapper;

    public List<GroupUserVO> list(Long realmId, Long groupId) {
        List<GroupUser> groupUsers = groupUserMapper.selectList(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getGroupId, groupId)
        );
        return EnhancedBeanUtils.createAndCopyList(groupUsers, GroupUserVO.class);
    }

    @Transactional
    public void batchUpdate(Long groupId, GroupUserBatchUpdateParam param) {
        if (CollectionUtils.isEmpty(param.getUserIds())) {
            return;
        }

        switch (param.getType()) {
            case SAVE: // 新增
                List<GroupUser> groupUsers = new ArrayList<>();
                for (Long userId : param.getUserIds()) {
                    GroupUser groupUser = new GroupUser();
                    groupUser.setGroupId(groupId);
                    groupUser.setUserId(userId);
                    groupUser.setExtend(param.getExtend());
                    groupUsers.add(groupUser);
                }
                groupUserMapper.batchInsertIgnore(groupUsers);
                break;
            case UPDATE:
                for (Long userId : param.getUserIds()) {
                    groupUserMapper.update(null, Wrappers.<GroupUser>lambdaUpdate()
                            .set(GroupUser::getExtend, param.getExtend())
                            .eq(GroupUser::getGroupId, groupId)
                            .eq(GroupUser::getUserId, userId)
                    );
                }
                break;
            case DELETE: // 删除
                groupUserMapper.delete(Wrappers.<GroupUser>lambdaQuery()
                        .eq(GroupUser::getGroupId, groupId)
                        .in(GroupUser::getUserId, param.getUserIds())
                );
                break;
            default:
                break;
        }
    }

    /**
     * 用户所在用户组
     */
    public List<GroupVO> checkGroup(Long userId, Long realmId, GroupCheckParam param) {
        List<GroupUser> userGroups = groupUserMapper.selectList(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getUserId, userId)
                .in(CollectionUtils.isNotEmpty(param.getGroupIds()), GroupUser::getGroupId, param.getGroupIds())
        );

        if (CollectionUtils.isEmpty(userGroups)) {
            return new ArrayList<>();
        }

        List<Long> groupIds = userGroups.stream().map(GroupUser::getGroupId).collect(Collectors.toList());
        List<Group> groups = groupMapper.selectList(Wrappers.<Group>lambdaQuery()
                .eq(Group::getRealmId, realmId)
                .in(Group::getGroupId, groupIds)
                .eq(StringUtils.isNotBlank(param.getName()), Group::getName, param.getName())
                .like(StringUtils.isNotBlank(param.getDimName()), Group::getName, param.getDimName())
                .in(CollectionUtils.isNotEmpty(param.getNames()), Group::getName, param.getNames())

        );
        if (CollectionUtils.isEmpty(groups)) {
            return new ArrayList<>();
        }

        return EnhancedBeanUtils.createAndCopyList(groups, GroupVO.class);
    }

    public List<GroupUserVO> getByUser(Long userId) {
        Verifies.notNull(userId, "用户ID不能为空");
        List<GroupUser> groupUsers = groupUserMapper.selectList(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getUserId, userId)
        );
        return EnhancedBeanUtils.createAndCopyList(groupUsers, GroupUserVO.class);
    }

    public GroupUserVO getByUserAndGroup(Long userId, Long groupId) {
        Verifies.notNull(userId, "用户ID不能为空");
        Verifies.notNull(groupId, "用户组ID不能为空");
        GroupUser groupUser = groupUserMapper.selectOne(Wrappers.<GroupUser>lambdaQuery()
                .eq(GroupUser::getUserId, userId)
                .eq(GroupUser::getGroupId, groupId)
        );
        return EnhancedBeanUtils.createAndCopy(groupUser, GroupUserVO.class);
    }

}
