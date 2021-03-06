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

package org.limbo.doorkeeper.server.infrastructure.checker;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.limbo.doorkeeper.api.constants.DoorkeeperConstants;
import org.limbo.doorkeeper.api.constants.Intention;
import org.limbo.doorkeeper.api.constants.Logic;
import org.limbo.doorkeeper.api.constants.UriMethod;
import org.limbo.doorkeeper.api.model.param.query.PermissionQueryParam;
import org.limbo.doorkeeper.api.model.param.query.PolicyCheckerParam;
import org.limbo.doorkeeper.api.model.param.query.ResourceCheckParam;
import org.limbo.doorkeeper.api.model.param.query.ResourceQueryParam;
import org.limbo.doorkeeper.api.model.vo.PermissionPolicyVO;
import org.limbo.doorkeeper.api.model.vo.PermissionVO;
import org.limbo.doorkeeper.api.model.vo.ResourceVO;
import org.limbo.doorkeeper.api.model.vo.check.ResourceCheckResult;
import org.limbo.doorkeeper.api.model.vo.policy.PolicyVO;
import org.limbo.doorkeeper.server.domain.PatternDO;
import org.limbo.doorkeeper.server.infrastructure.dao.PolicyDao;
import org.limbo.doorkeeper.server.infrastructure.exception.AuthorizationException;
import org.limbo.doorkeeper.server.infrastructure.mapper.*;
import org.limbo.doorkeeper.server.infrastructure.po.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ???????????????
 *
 * @author Devil
 * @date 2021/3/31 2:04 ??????
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

    @Autowired
    private UriMapper uriMapper;

    /**
     * ?????????????????????
     */
    @Autowired
    private PolicyCheckerFactory policyCheckerFactory;

    /**
     * ???????????????????????????????????????????????????????????????????????????Permission
     */
    private boolean refuseWhenUnauthorized = true;

    /**
     * ??????????????????????????????????????????
     *
     * @param userId     ??????id
     * @param checkParam ???????????????????????????
     * @return
     */
    public ResourceCheckResult check(Long userId, ResourceCheckParam checkParam) {
        ClientPO client = getClient(checkParam.getClientId());

        UserPO user = getUser(userId);
        if (!user.getIsEnabled()) {
            return emptyResult();
        }

        try {
            // ??????????????????????????????
            List<ResourceVO> resources = findResources(client.getRealmId(), client.getClientId(), checkParam);
            if (CollectionUtils.isEmpty(resources)) {
                return emptyResult();
            }

            // ????????????????????????
            List<PermissionResourcePO> permissionResources = permissionResourceMapper.selectList(Wrappers.<PermissionResourcePO>lambdaQuery()
                    .in(PermissionResourcePO::getResourceId, resources.stream().map(ResourceVO::getResourceId).collect(Collectors.toList()))
            );
            if (CollectionUtils.isEmpty(permissionResources)) {
                return checkResourceRefuseResult(resources);
            }
            Set<Long> permissionIds = new HashSet<>();
            Map<Long, List<Long>> resourcePermissionMap = new HashMap<>();
            for (PermissionResourcePO permissionResource : permissionResources) {
                permissionIds.add(permissionResource.getPermissionId());
                if (!resourcePermissionMap.containsKey(permissionResource.getResourceId())) {
                    resourcePermissionMap.put(permissionResource.getResourceId(), new ArrayList<>());
                }
                resourcePermissionMap.get(permissionResource.getResourceId()).add(permissionResource.getPermissionId());
            }

            // ????????????
            List<PermissionVO> allPermissions = getPermissions(client.getRealmId(), client.getClientId(), new ArrayList<>(permissionIds));
            if (CollectionUtils.isEmpty(allPermissions)) {
                return checkResourceRefuseResult(resources);
            }

            // ????????????ID
            Map<Long, PermissionVO> permissionMap = new HashMap<>();
            Set<Long> policyIds = new HashSet<>();
            for (PermissionVO permission : allPermissions) {
                if (Logic.parse(permission.getLogic()) == null) {
                    throw new IllegalArgumentException("??????????????????????????????permission=" + permission);
                }
                permissionMap.put(permission.getPermissionId(), permission);
                if (CollectionUtils.isNotEmpty(permission.getPolicies())) {
                    policyIds.addAll(permission.getPolicies().stream().map(PermissionPolicyVO::getPolicyId).collect(Collectors.toList()));
                }
            }
            if (CollectionUtils.isEmpty(policyIds)) {
                return checkResourceRefuseResult(resources);
            }

            // ????????????
            List<PolicyVO> allPolicies = policyDao.getVOSByPolicyIds(client.getRealmId(), client.getClientId(), new ArrayList<>(policyIds), true);
            if (CollectionUtils.isEmpty(allPolicies)) {
                return checkResourceRefuseResult(resources);
            }
            Map<Long, PolicyVO> policyMap = allPolicies.stream().collect(Collectors.toMap(PolicyVO::getPolicyId, policyVO -> policyVO));

            // ?????????????????????
            PolicyChecker checker = policyCheckerFactory.newPolicyChecker(user);

            List<ResourceVO> result = new ArrayList<>();
            ASSIGNER_ITER:
            for (ResourceVO resource : resources) {
                // ??????????????????ID
                List<Long> resourcePermissionIds = resourcePermissionMap.get(resource.getResourceId());
                if (CollectionUtils.isEmpty(resourcePermissionIds)) {
                    if (refuseWhenUnauthorized) {
                        continue;
                    } else {
                        result.add(resource);
                    }
                }

                // ??????????????????
                List<PermissionVO> permissionVOS = new ArrayList<>();
                for (Long permissionId : resourcePermissionIds) {
                    if (permissionMap.containsKey(permissionId)) {
                        permissionVOS.add(permissionMap.get(permissionId));
                    }
                }
                if (CollectionUtils.isEmpty(permissionVOS)) {
                    if (refuseWhenUnauthorized) {
                        continue;
                    } else {
                        result.add(resource);
                    }
                }

                // ???Permission???Intention????????????
                Map<Intention, Set<PermissionVO>> intentGroupedPerms = permissionVOS.stream().collect(Collectors.groupingBy(
                        permissionVO -> Intention.parse(permissionVO.getIntention()),
                        Collectors.mapping(Function.identity(), Collectors.toSet())
                ));

                // ????????? REFUSE ?????????????????????????????? REFUSE ?????????????????????????????????????????????????????????
                Set<PermissionVO> refusedPerms = intentGroupedPerms.getOrDefault(Intention.REFUSE, new HashSet<>());
                for (PermissionVO permission : refusedPerms) {
                    if (checkPermissionLogic(checker, checkParam, permission, policyMap)) {
                        continue ASSIGNER_ITER;
                    }
                }
                // ????????? ALLOW ?????????
                Set<PermissionVO> allowedPerms = intentGroupedPerms.getOrDefault(Intention.ALLOW, new HashSet<>());
                for (PermissionVO permission : allowedPerms) {
                    if (checkPermissionLogic(checker, checkParam, permission, policyMap)) {
                        result.add(resource);
                        continue ASSIGNER_ITER;
                    }
                }

            }
            return new ResourceCheckResult(result);

        } catch (Exception e) {
            log.error("??????????????????", e);
            throw new AuthorizationException(e.getMessage());
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @return ??????????????????
     */
    private List<ResourceVO> findResources(Long realmId, Long clientId, ResourceCheckParam checkParam) {
        List<Long> resourceIds = checkParam.getResourceIds();
        if (CollectionUtils.isEmpty(resourceIds)) {
            resourceIds = new ArrayList<>();
        }
        // ??????uri??????id
        if (CollectionUtils.isNotEmpty(checkParam.getUris())) {
            List<Long> uriIds = new ArrayList<>();

            // client???????????????uri??????
            List<UriPO> clientUris = uriMapper.selectList(Wrappers.<UriPO>lambdaQuery()
                    .eq(UriPO::getRealmId, realmId)
                    .eq(UriPO::getClientId, clientId)
            );

            // ??????????????????????????????????????????ID
            // ???????????????uri ???????????? checkParam ?????????????????? ?????????????????????????????????
            for (UriPO uri : clientUris) {
                for (String str : checkParam.getUris()) {
                    String requestMethod = UriMethod.ALL.getValue();
                    String requestUri;
                    if (str.contains(DoorkeeperConstants.KV_DELIMITER)) {
                        String[] split = str.split(DoorkeeperConstants.KV_DELIMITER);
                        requestMethod = split[0];
                        requestUri = split[1];
                    } else {
                        requestUri = str;
                    }

                    // ?????????????????????????????????
                    PatternDO pattern = new PatternDO(uri.getUri().trim());
                    if ((UriMethod.ALL == uri.getMethod() || uri.getMethod() == UriMethod.parse(requestMethod))
                            && pattern.pathMatch(requestUri.trim())) {
                        uriIds.add(uri.getUriId());
                        break;
                    }
                }
            }

            // ?????????????????????????????? ????????????
            if (CollectionUtils.isEmpty(uriIds)) {
                return new ArrayList<>();
            }

            List<ResourceUriPO> resourceUris = resourceUriMapper.selectList(Wrappers.<ResourceUriPO>lambdaQuery()
                    .in(ResourceUriPO::getUriId, uriIds)
            );
            resourceIds.addAll(resourceUris.stream().map(ResourceUriPO::getResourceId).collect(Collectors.toList()));
            // ?????????????????????????????? ????????????
            if (CollectionUtils.isEmpty(resourceIds)) {
                return new ArrayList<>();
            }

        }

        ResourceQueryParam param = new ResourceQueryParam();
        param.setRealmId(realmId);
        param.setClientId(clientId);
        param.setResourceIds(resourceIds);
        param.setNames(checkParam.getNames());
        param.setAndKvs(checkParam.getAndTags());
        param.setOrKvs(checkParam.getOrTags());
        param.setIsEnabled(true);
        param.setNeedAll(true);
        param.setNeedTag(checkParam.getNeedTag());
        param.setNeedUri(checkParam.getNeedUri());
        param.setNeedParentId(checkParam.getNeedParentId());
        param.setNeedChildrenId(checkParam.getNeedChildrenId());
        return resourceMapper.getVOS(param);
    }


    /**
     * ???????????????????????????
     *
     * @return ???????????????????????????
     */
    private List<PermissionVO> findResourcePermissions(Long realmId, Long clientId, Long resourceId) {
        List<PermissionResourcePO> permissionResources = permissionResourceMapper.selectList(Wrappers.<PermissionResourcePO>lambdaQuery()
                .eq(PermissionResourcePO::getResourceId, resourceId)
        );
        if (CollectionUtils.isEmpty(permissionResources)) {
            return new ArrayList<>();
        }
        List<Long> permissionIds = permissionResources.stream().map(PermissionResourcePO::getPermissionId).collect(Collectors.toList());
        PermissionQueryParam param = new PermissionQueryParam();
        param.setRealmId(realmId);
        param.setClientId(clientId);
        param.setPermissionIds(permissionIds);
        param.setIsEnabled(true);
        param.setNeedAll(true);
        return permissionMapper.getVOS(param);
    }


    /**
     * ??????Permission?????????
     *
     * @param permission ????????????????????????
     * @return ??????Permission??????????????????
     */
    private boolean checkPermissionLogic(PolicyChecker checker, ResourceCheckParam checkParam, PermissionVO permission,
                                         Map<Long, PolicyVO> policyMap) {
        // ????????????????????????
        if (!permission.getIsEnabled()) {
            return false;
        }

        Logic logic = Logic.parse(permission.getLogic());
        if (logic == null) {
            throw new IllegalArgumentException("??????????????????????????????permission=" + permission);
        }

        if (CollectionUtils.isEmpty(permission.getPolicies())) {
            return false;
        }

        // ??????policy??????
        int allowedCount = 0;
        int totalCount = 0;
        for (PermissionPolicyVO permissionPolicy : permission.getPolicies()) {
            PolicyVO policy = policyMap.get(permissionPolicy.getPolicyId());
            if (policy == null) {
                continue;
            }
            // todo ???????????????????????????
//            PolicyChecker policyChecker = policyCheckerFactory.newPolicyChecker(user, policy);
//            if (policyChecker == null) {
//                continue;
//            }
            PolicyCheckerParam policyCheckerParam = new PolicyCheckerParam();
            policyCheckerParam.setParams(checkParam.getParams());
            totalCount++;
            // ???????????????policy??????
            if (checker.check(policy, policyCheckerParam.getParams()).allow()) {
                allowedCount++;
            }

        }

        return Logic.isSatisfied(logic, totalCount, allowedCount);
    }

    /**
     * ??????client
     *
     * @param clientId
     * @return
     */
    private ClientPO getClient(Long clientId) {
        ClientPO client = clientMapper.selectById(clientId);
        if (client == null) {
            throw new AuthorizationException("????????????Client???clientId=" + clientId);
        }
        if (!client.getIsEnabled()) {
            throw new AuthorizationException("???Client?????????");
        }
        return client;
    }

    private UserPO getUser(Long userId) {
        UserPO user = userMapper.selectById(userId);
        if (user == null) {
            throw new AuthorizationException("?????????????????????Id=" + userId);
        }
        return user;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param resources
     * @return
     */
    private ResourceCheckResult checkResourceRefuseResult(List<ResourceVO> resources) {
        if (!refuseWhenUnauthorized) {
            return new ResourceCheckResult(resources);
        }
        return new ResourceCheckResult(new ArrayList<>());
    }

    /**
     * ???????????????
     *
     * @return
     */
    private ResourceCheckResult emptyResult() {
        return new ResourceCheckResult(new ArrayList<>());
    }

    private List<PermissionVO> getPermissions(Long realmId, Long clientId, List<Long> permissionIds) {
        PermissionQueryParam param = new PermissionQueryParam();
        param.setRealmId(realmId);
        param.setClientId(clientId);
        param.setPermissionIds(permissionIds);
        param.setIsEnabled(true);
        param.setNeedAll(true);
        return permissionMapper.getVOS(param);
    }
}
