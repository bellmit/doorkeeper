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

package org.limbo.doorkeeper.server.support.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.limbo.doorkeeper.api.constants.DoorkeeperConstants;
import org.limbo.doorkeeper.api.constants.HttpMethod;
import org.limbo.doorkeeper.api.constants.Intention;
import org.limbo.doorkeeper.api.constants.Logic;
import org.limbo.doorkeeper.api.model.param.check.ResourceCheckParam;
import org.limbo.doorkeeper.api.model.param.permission.PermissionQueryParam;
import org.limbo.doorkeeper.api.model.param.resource.ResourceQueryParam;
import org.limbo.doorkeeper.api.model.vo.PermissionPolicyVO;
import org.limbo.doorkeeper.api.model.vo.PermissionVO;
import org.limbo.doorkeeper.api.model.vo.ResourceVO;
import org.limbo.doorkeeper.api.model.vo.check.ResourceCheckResult;
import org.limbo.doorkeeper.api.model.vo.policy.PolicyVO;
import org.limbo.doorkeeper.server.dal.dao.PolicyDao;
import org.limbo.doorkeeper.server.dal.entity.Client;
import org.limbo.doorkeeper.server.dal.entity.PermissionResource;
import org.limbo.doorkeeper.server.dal.entity.ResourceUri;
import org.limbo.doorkeeper.server.dal.entity.User;
import org.limbo.doorkeeper.server.dal.mapper.*;
import org.limbo.doorkeeper.server.support.auth.policies.PolicyChecker;
import org.limbo.doorkeeper.server.support.auth.policies.PolicyCheckerFactory;
import org.limbo.doorkeeper.server.utils.EasyAntPathMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资源校验器
 *
 * @author Devil
 * @date 2021/3/31 2:04 下午
 */
@Slf4j
@Component
public class ResourceChecker {

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private PolicyDao policyDao;

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private PermissionResourceMapper permissionResourceMapper;

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private ResourceUriMapper resourceUriMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 策略检查器工厂
     */
    @Autowired
    private PolicyCheckerFactory policyCheckerFactory;

    /**
     * 未授权情况下是否拒绝，默认ture。
     * 未授权是指，资源找不到对应的Permission
     */
    @Setter
    private boolean refuseWhenUnauthorized = true;

    private static final ThreadLocal<EasyAntPathMatcher> PATH_MATCHER = ThreadLocal.withInitial(EasyAntPathMatcher::new);

    /**
     * 校验参数
     */
    private ResourceCheckParam checkParam;

    /**
     * 校验参数中clientId对应的client
     */
    private Client client;

    private User user;

    /**
     * 进行权限校验，是否有资格访问
     *
     * @return 校验结果，分为refused和allowed两类
     */
    public ResourceCheckResult check(Long userId, ResourceCheckParam checkParam) {
        initCheck(userId, checkParam);

        try {
            List<ResourceVO> result = new ArrayList<>();

            // 找到待检测的启用资源
            List<ResourceVO> findResources = findResources();
            ASSIGNER_ITER:
            for (ResourceVO findResource : findResources) {
                // 遍历资源依次拿到权限Permission
                List<PermissionVO> permissions = findResourcePermissions(findResource.getResourceId());

                // 未授权的情况
                if (CollectionUtils.isEmpty(permissions)) {
                    if (refuseWhenUnauthorized) {
                        continue;
                    } else {
                        result.add(findResource);
                    }
                }

                // 对Permission的Intention进行分组
                Map<Intention, Set<PermissionVO>> intentGroupedPerms = permissions.stream().collect(Collectors.groupingBy(
                        permissionVO -> Intention.parse(permissionVO.getIntention()),
                        Collectors.mapping(Function.identity(), Collectors.toSet())
                ));
                // 先检测 REFUSE 的权限，如果存在一个 REFUSE 的权限校验通过，则此资源约束被看作拒绝
                Set<PermissionVO> refusedPerms = intentGroupedPerms.getOrDefault(Intention.REFUSE, new HashSet<>());
                for (PermissionVO permission : refusedPerms) {
                    if (checkPermissionLogic(permission)) {
                        continue ASSIGNER_ITER;
                    }
                }
                // 再检测 ALLOW 的权限
                Set<PermissionVO> allowedPerms = intentGroupedPerms.getOrDefault(Intention.ALLOW, new HashSet<>());
                for (PermissionVO permission : allowedPerms) {
                    if (checkPermissionLogic(permission)) {
                        result.add(findResource);
                        continue ASSIGNER_ITER;
                    }
                }
            }

            return new ResourceCheckResult(result);

        } catch (Exception e) {
            log.error("鉴权校验失败", e);
            throw new AuthorizationException(e.getMessage());
        }
    }

    private void initCheck(Long userId, ResourceCheckParam checkParam) {
        this.client = clientMapper.selectById(checkParam.getClientId());
        if (client == null) {
            throw new AuthorizationException("无法找到Client，clientId=" + checkParam.getClientId());
        }
        if (!client.getIsEnabled()) {
            throw new AuthorizationException("此Client未启用");
        }
        this.user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthorizationException("无法找到用户，Id=" + userId);
        }
        if (!user.getIsEnabled()) {
            throw new AuthorizationException("此用户未启用");
        }
        user.setPassword(null);

        this.checkParam = checkParam;
    }

    /**
     * 根据参数获取需要校验的资源
     *
     * @return 返回资源列表
     */
    private List<ResourceVO> findResources() {
        List<Long> resourceIds = new ArrayList<>();
        // 获取uri资源id
        if (CollectionUtils.isNotEmpty(checkParam.getUris())) {
            // client拥有的全部uri资源
            List<ResourceUri> clientUris = resourceUriMapper.selectList(Wrappers.<ResourceUri>lambdaQuery()
                    .eq(ResourceUri::getRealmId, client.getRealmId())
                    .eq(ResourceUri::getClientId, client.getClientId())
            );
            // 根据路径和请求方式，获取资源ID
            for (String str : checkParam.getUris()) {
                for (ResourceUri resourceUri : clientUris) {
                    String requestMethod = HttpMethod.ALL.getValue();
                    String requestUri;
                    if (str.contains(DoorkeeperConstants.KV_DELIMITER)) {
                        String[] split = str.split(DoorkeeperConstants.KV_DELIMITER);
                        requestMethod = split[0];
                        requestUri = split[1];
                    } else {
                        requestUri = str;
                    }

                    // 判断是否匹配方法和路径
                    if (resourceUri.getMethod() == HttpMethod.parse(requestMethod) && pathMatch(resourceUri.getUri().trim(), requestUri.trim())) {
                        resourceIds.add(resourceUri.getResourceId());
                        break;
                    }
                }
            }

            // 如果匹配到的资源为空 则返回空
            if (CollectionUtils.isEmpty(resourceIds)) {
                return new ArrayList<>();
            }

        }
        ResourceQueryParam param = new ResourceQueryParam();
        param.setRealmId(client.getRealmId());
        param.setClientId(client.getClientId());
        param.setResourceIds(resourceIds);
        param.setNames(checkParam.getNames());
        param.setKvs(checkParam.getTags());
        param.setIsEnabled(true);
        param.setNeedAll(true);
        return resourceMapper.getVOS(param);
    }


    /**
     * 找到资源关联的权限
     *
     * @return 资源关联的权限列表
     */
    private List<PermissionVO> findResourcePermissions(Long resourceId) {
        List<PermissionResource> permissionResources = permissionResourceMapper.selectList(Wrappers.<PermissionResource>lambdaQuery()
                .eq(PermissionResource::getResourceId, resourceId)
        );
        if (CollectionUtils.isEmpty(permissionResources)) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = permissionResources.stream().map(PermissionResource::getPermissionId).collect(Collectors.toList());
        PermissionQueryParam param = new PermissionQueryParam();
        param.setRealmId(client.getRealmId());
        param.setClientId(client.getClientId());
        param.setPermissionIds(permissionIds);
        param.setIsEnabled(true);
        param.setNeedAll(true);
        return permissionMapper.getVOS(param);
    }


    /**
     * 进行Permission的校验
     *
     * @param permission 待校验的授权信息
     * @return 返回Permission校验是否通过
     */
    private boolean checkPermissionLogic(PermissionVO permission) {
        // 检测权限是否禁用
        if (!permission.getIsEnabled()) {
            return false;
        }

        Logic logic = Logic.parse(permission.getLogic());
        if (logic == null) {
            throw new IllegalArgumentException("无法解析权限的策略，permission=" + permission);
        }

        List<PermissionPolicyVO> permissionPolicies = permission.getPolicies();
        if (CollectionUtils.isEmpty(permissionPolicies)) {
            return false;
        }

        // 逐个policy检查
        List<Long> policyIds = permissionPolicies.stream().map(PermissionPolicyVO::getPolicyId).collect(Collectors.toList());
        List<PolicyVO> policies = policyDao.getVOSByPolicyIds(client.getRealmId(), client.getClientId(), policyIds, true);
        if (CollectionUtils.isEmpty(policies)) {
            return false;
        }

        int allowedCount = 0;
        int totalCount = 0;
        for (PolicyVO policy : policies) {
            PolicyChecker policyChecker = policyCheckerFactory.newPolicyChecker(user, policy);
            if (policyChecker == null) {
                continue;
            }

            Intention policyCheckIntention = policyChecker.check(checkParam);
            totalCount++;
            // 统计允许的policy个数
            if (Intention.ALLOW == policyCheckIntention) {
                allowedCount++;
            }
        }

        return LogicChecker.isSatisfied(logic, totalCount, allowedCount);
    }

    /**
     * 判断path是否符合ant风格的pattern
     *
     * @param pattern ant风格的路径pattern
     * @param path    访问的路径
     * @return 是否匹配
     */
    public boolean pathMatch(String pattern, String path) {
        EasyAntPathMatcher antPathMatcher = PATH_MATCHER.get();
        return antPathMatcher.match(StringUtils.trim(pattern), StringUtils.trim(path));
    }
}