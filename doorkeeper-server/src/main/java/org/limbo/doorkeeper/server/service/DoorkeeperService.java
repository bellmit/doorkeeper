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

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.limbo.doorkeeper.api.constants.*;
import org.limbo.doorkeeper.api.model.param.InitParam;
import org.limbo.doorkeeper.api.model.param.add.*;
import org.limbo.doorkeeper.api.model.param.batch.UserRoleBatchUpdateParam;
import org.limbo.doorkeeper.api.model.param.query.*;
import org.limbo.doorkeeper.api.model.param.update.PermissionUpdateParam;
import org.limbo.doorkeeper.api.model.vo.*;
import org.limbo.doorkeeper.api.model.vo.check.ResourceCheckResult;
import org.limbo.doorkeeper.api.model.vo.check.RoleCheckResult;
import org.limbo.doorkeeper.server.infrastructure.exception.ParamException;
import org.limbo.doorkeeper.server.infrastructure.mapper.*;
import org.limbo.doorkeeper.server.infrastructure.mapper.policy.PolicyMapper;
import org.limbo.doorkeeper.server.infrastructure.po.*;
import org.limbo.doorkeeper.server.infrastructure.utils.*;
import org.limbo.doorkeeper.server.infrastructure.checker.ResourceChecker;
import org.limbo.doorkeeper.server.infrastructure.checker.RoleChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * doorkeeper?????????????????????
 *
 * @author Devil
 * @date 2021/1/10 10:39 ??????
 */
@Slf4j
@Service
public class DoorkeeperService {

    @Autowired
    private RealmMapper realmMapper;

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private PolicyMapper policyMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private ResourceChecker resourceChecker;

    @Autowired
    private RoleChecker roleChecker;

    private volatile String realmResource;

    private volatile String clientResource;
    /**
     * doorkeeper??????????????? ????????????secret?????????
     */
    private volatile RealmPO doorkeeperRealm;

    /**
     * ?????????????????????
     */
    @Transactional
    public void initDoorkeeper(InitParam param) {
        RealmPO realm = new RealmPO();
        realm.setName(DoorkeeperConstants.DOORKEEPER_REALM_NAME);
        realm.setSecret(UUIDUtils.get());
        try {
            realmMapper.insert(realm);
        } catch (DuplicateKeyException e) {
            throw new ParamException("??????????????????");
        }

        // ?????????????????????
        UserPO admin = new UserPO();
        admin.setRealmId(realm.getRealmId());
        admin.setNickname(DoorkeeperConstants.ADMIN);
        admin.setUsername(StringUtils.isBlank(param.getUsername()) ? DoorkeeperConstants.ADMIN : param.getUsername());
        admin.setPassword(MD5Utils.md5WithSalt(StringUtils.isBlank(param.getPassword()) ? DoorkeeperConstants.ADMIN : param.getPassword()));
        admin.setIsEnabled(true);
        userMapper.insert(admin);
        // ??????doorkeeper????????????
        RoleAddParam realmAdminRoleParam = createRole(DoorkeeperConstants.REALM_CLIENT_ID, DoorkeeperConstants.ADMIN, "");
        RoleVO realmAdminRole = roleService.add(realm.getRealmId(), realmAdminRoleParam);
        // ????????????????????????
        UserRoleBatchUpdateParam userRoleBatchUpdateParam = new UserRoleBatchUpdateParam();
        userRoleBatchUpdateParam.setType(BatchMethod.SAVE);
        userRoleBatchUpdateParam.setRoleIds(Collections.singletonList(realmAdminRole.getRoleId()));
        userRoleService.batchUpdate(admin.getUserId(), userRoleBatchUpdateParam);
        // ??????api client
        ClientPO apiClient = new ClientPO();
        apiClient.setRealmId(realm.getRealmId());
        apiClient.setName(DoorkeeperConstants.API_CLIENT);
        apiClient.setDescription("manager doorkeeper permission");
        apiClient.setIsEnabled(true);
        clientMapper.insert(apiClient);
        // ????????????
        createRealmResource(admin.getUserId(), realm.getRealmId(), realm.getName());
        createClientResource(admin.getUserId(), realm.getRealmId(), apiClient.getClientId(), apiClient.getName());
    }

    @Transactional
    public RealmVO addRealm(Long userId, RealmAddParam param) {
        RealmPO realm = EnhancedBeanUtils.createAndCopy(param, RealmPO.class);
        if (StringUtils.isBlank(param.getSecret())) {
            realm.setSecret(UUIDUtils.get());
        }
        try {
            realmMapper.insert(realm);
        } catch (DuplicateKeyException e) {
            throw new ParamException("????????????");
        }

        // ?????????realm??????
        createRealmResource(userId, realm.getRealmId(), realm.getName());

        return EnhancedBeanUtils.createAndCopy(realm, RealmVO.class);
    }

    /**
     * user????????????realm
     */
    public List<RealmVO> userRealms(Long userId) {
        LambdaQueryWrapper<RealmPO> realmSelect = Wrappers.<RealmPO>lambdaQuery().select(RealmPO::getRealmId, RealmPO::getName);
        // ???????????????doorkeeper???REALM admin
        if (isSuperAdmin(userId)) {
            List<RealmPO> realms = realmMapper.selectList(realmSelect);
            return EnhancedBeanUtils.createAndCopyList(realms, RealmVO.class);
        }

        ClientPO apiClient = clientMapper.getByName(getDoorkeeperRealmId(), DoorkeeperConstants.API_CLIENT);
        // ??????????????????????????????realm ??????
        ResourceCheckParam checkParam = new ResourceCheckParam();
        checkParam.setClientId(apiClient.getClientId());
        checkParam.setOrTags(Collections.singletonList("type=realmOwn"));
        checkParam.setNeedTag(true);
        ResourceCheckResult check = resourceChecker.check(userId, checkParam);
        if (CollectionUtils.isEmpty(check.getResources())) {
            return new ArrayList<>();
        }

        List<Long> realmIds = new ArrayList<>();
        for (ResourceVO resource : check.getResources()) {
            if (CollectionUtils.isEmpty(resource.getTags())) {
                continue;
            }
            for (ResourceTagVO tag : resource.getTags()) {
                if (DoorkeeperConstants.REALM_ID.equals(tag.getK())) {
                    realmIds.add(Long.valueOf(tag.getV()));
                    break;
                }
            }
        }

        List<RealmPO> realms = realmMapper.selectList(realmSelect
                .in(RealmPO::getRealmId, realmIds)
        );
        return EnhancedBeanUtils.createAndCopyList(realms, RealmVO.class);
    }

    @Transactional
    public ClientVO addClient(Long realmId, Long userId, ClientAddParam param) {
        ClientPO client = EnhancedBeanUtils.createAndCopy(param, ClientPO.class);
        client.setRealmId(realmId);
        try {
            clientMapper.insert(client);
        } catch (DuplicateKeyException e) {
            throw new ParamException("??????????????????");
        }

        // ?????????client??????
        createClientResource(userId, realmId, client.getClientId(), client.getName());

        return EnhancedBeanUtils.createAndCopy(client, ClientVO.class);
    }

    /**
     * user????????????client
     */
    public List<ClientVO> userClients(Long realmId, Long userId, ClientQueryParam param) {
        List<Long> clientIds = null;
        // ???????????????doorkeeper???REALM admin
        if (!isSuperAdmin(userId)) {
            clientIds = new ArrayList<>();
            // ??????realm???doorkeeper????????????client
            ClientPO apiClient = clientMapper.getByName(getDoorkeeperRealmId(), DoorkeeperConstants.API_CLIENT);

            ResourceCheckParam checkParam = new ResourceCheckParam();
            checkParam.setClientId(apiClient.getClientId());
            checkParam.setOrTags(Collections.singletonList("type=clientOwn"));
            checkParam.setNeedTag(true);
            ResourceCheckResult check = resourceChecker.check(userId, checkParam);
            if (CollectionUtils.isEmpty(check.getResources())) {
                return new ArrayList<>();
            }

            for (ResourceVO resource : check.getResources()) {
                if (CollectionUtils.isEmpty(resource.getTags())) {
                    continue;
                }
                for (ResourceTagVO tag : resource.getTags()) {
                    if (DoorkeeperConstants.CLIENT_ID.equals(tag.getK())) {
                        clientIds.add(Long.valueOf(tag.getV()));
                        break;
                    }
                }
            }
        }

        List<ClientPO> clients = clientMapper.selectList(Wrappers.<ClientPO>lambdaQuery()
                .eq(ClientPO::getRealmId, realmId)
                .eq(StringUtils.isNotBlank(param.getName()), ClientPO::getName, param.getName())
                .like(StringUtils.isNotBlank(param.getDimName()), ClientPO::getName, param.getDimName())
                .in(clientIds != null, ClientPO::getClientId, clientIds)
                .orderByDesc(ClientPO::getClientId)
        );
        return EnhancedBeanUtils.createAndCopyList(clients, ClientVO.class);
    }

    /**
     * ????????? ???doorkeeper ??????????????? realmName???client ?????????realm ?????? ???????????????
     *
     * @param userId    ?????????ID
     * @param realmId   ?????????RealmId
     * @param realmName ?????????realm??????
     */
    @Transactional
    public void createRealmResource(Long userId, Long realmId, String realmName) {
        ClientPO apiClient = clientMapper.getByName(getDoorkeeperRealmId(), DoorkeeperConstants.API_CLIENT);
        // ?????????
        String resourceTemplate = getRealmResourceTemplate();
        resourceTemplate = resourceTemplate.replaceAll("\\$\\{realmId}", realmId.toString());
        resourceTemplate = resourceTemplate.replaceAll("\\$\\{realmName}", realmName);
        List<ResourceAddParam> resourceAddParams = JacksonUtil.parseObject(resourceTemplate, new TypeReference<List<ResourceAddParam>>() {
        });
        for (ResourceAddParam resourceAddParam : resourceAddParams) {
            resourceService.add(getDoorkeeperRealmId(), apiClient.getClientId(), resourceAddParam);
        }

        UserPO user = userMapper.selectById(userId);
        List<String> resourceNames = new ArrayList<>();
        resourceNames.add(realmName + "-realm-join");
        resourceNames.add(realmName + "-realm-manager");
        bindUser(user.getUserId(), user.getUsername(), resourceNames, apiClient.getRealmId(), apiClient.getClientId());
    }

    /**
     * ??????client ???doorkeeper ??????client?????? realmName???client????????????????????? ??????????????????
     *
     * @param userId     ?????????ID
     * @param realmId    ??????client???realmId
     * @param clientId   ?????????clientId
     * @param clientName ?????????clientName
     */
    @Transactional
    public void createClientResource(Long userId, Long realmId, Long clientId, String clientName) {
        ClientPO apiClient = clientMapper.getByName(getDoorkeeperRealmId(), DoorkeeperConstants.API_CLIENT);
        RealmPO realm = realmMapper.selectById(realmId);

        // ??????
        String resourceTemplate = getClientResourceTemplate();
        resourceTemplate = resourceTemplate.replaceAll("\\$\\{realmId}", realmId.toString());
        resourceTemplate = resourceTemplate.replaceAll("\\$\\{realmName}", realm.getName());
        resourceTemplate = resourceTemplate.replaceAll("\\$\\{clientId}", clientId.toString());
        resourceTemplate = resourceTemplate.replaceAll("\\$\\{clientName}", clientName);
        List<ResourceAddParam> resourceAddParams = JacksonUtil.parseObject(resourceTemplate, new TypeReference<List<ResourceAddParam>>() {
        });
        for (ResourceAddParam resourceAddParam : resourceAddParams) {
            resourceService.add(getDoorkeeperRealmId(), apiClient.getClientId(), resourceAddParam);
        }

        UserPO user = userMapper.selectById(userId);
        List<String> resourceNames = new ArrayList<>();
        resourceNames.add(realm.getName() + "-" + clientName + "-client-join");
        resourceNames.add(realm.getName() + "-" + clientName + "-client-manager");
        bindUser(user.getUserId(), user.getUsername(), resourceNames, apiClient.getRealmId(), apiClient.getClientId());
    }

    /**
     * ??????????????????/??????
     *
     * @param userId        ??????ID
     * @param username      ????????????
     * @param resourceNames ???????????????????????????
     * @param realmId       doorkeeper realmId
     * @param clientId      doorkeeper api clientId
     */
    public void bindUser(Long userId, String username, List<String> resourceNames, Long realmId, Long clientId) {
        String uqName = username + "-user";
        // ????????????
        Wrapper<PolicyPO> policyWrapper = Wrappers.<PolicyPO>lambdaQuery()
                .eq(PolicyPO::getRealmId, realmId)
                .eq(PolicyPO::getClientId, clientId)
                .eq(PolicyPO::getName, uqName);

        PolicyPO policy = policyMapper.selectOne(policyWrapper);
        if (policy == null) {
            try {
                PolicyAddParam policyParam = createUserPolicy(uqName, userId);
                policyService.add(realmId, clientId, policyParam);
            } catch (DuplicateKeyException e) {
                // ?????????????????????
            }
            policy = policyMapper.selectOne(policyWrapper);
        }

        // ??????
        List<ResourceVO> resources = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(resourceNames)) {
            ResourceQueryParam resourceQueryParam = new ResourceQueryParam();
            resourceQueryParam.setRealmId(realmId);
            resourceQueryParam.setClientId(clientId);
            resourceQueryParam.setNames(resourceNames);
            resources = resourceMapper.getVOS(resourceQueryParam);
        }
        List<Long> resourceIds = resources.stream().map(ResourceVO::getResourceId).collect(Collectors.toList());

        PermissionQueryParam permissionQueryParam = new PermissionQueryParam();
        permissionQueryParam.setRealmId(realmId);
        permissionQueryParam.setClientId(clientId);
        permissionQueryParam.setNames(Collections.singletonList(uqName));
        List<PermissionVO> vos = permissionMapper.getVOS(permissionQueryParam);
        if (CollectionUtils.isEmpty(vos)) {
            PermissionAddParam realmAdminPermissionParam = createPermission(uqName, resourceIds, policy.getPolicyId());
            permissionService.add(realmId, clientId, realmAdminPermissionParam);
        } else {
            PermissionUpdateParam permissionUpdateParam = new PermissionUpdateParam();
            if (CollectionUtils.isNotEmpty(vos.get(0).getPolicies())) {
                permissionUpdateParam.setPolicyIds(vos.get(0).getPolicies().stream().map(PermissionPolicyVO::getPolicyId).collect(Collectors.toList()));
            }
            if (CollectionUtils.isNotEmpty(vos.get(0).getResources())) {
                resourceIds.addAll(vos.get(0).getResources().stream().map(PermissionResourceVO::getResourceId).collect(Collectors.toList()));
            }
            permissionUpdateParam.setResourceIds(resourceIds);
            permissionService.update(realmId, clientId, vos.get(0).getPermissionId(), permissionUpdateParam);
        }
    }


    private RoleAddParam createRole(Long clientId, String name, String description) {
        RoleAddParam roleAddParam = new RoleAddParam();
        roleAddParam.setName(name);
        roleAddParam.setClientId(clientId);
        roleAddParam.setDescription(description);
        roleAddParam.setIsEnabled(true);
        return roleAddParam;
    }

    private PolicyAddParam createRolePolicy(String name, Long roleId) {
        PolicyAddParam policyAddParam = new PolicyAddParam();
        policyAddParam.setName(name);
        policyAddParam.setType(PolicyType.ROLE);
        policyAddParam.setLogic(Logic.ALL);
        policyAddParam.setIntention(Intention.ALLOW);
        policyAddParam.setIsEnabled(Boolean.TRUE);

        PolicyRoleAddParam roleAddParam = new PolicyRoleAddParam();
        roleAddParam.setRoleId(roleId);

        policyAddParam.setRoles(Collections.singletonList(roleAddParam));
        return policyAddParam;
    }

    private PolicyAddParam createUserPolicy(String name, Long userId) {
        PolicyAddParam policyAddParam = new PolicyAddParam();
        policyAddParam.setName(name);
        policyAddParam.setType(PolicyType.USER);
        policyAddParam.setLogic(Logic.ALL);
        policyAddParam.setIntention(Intention.ALLOW);
        policyAddParam.setIsEnabled(Boolean.TRUE);

        PolicyUserAddParam userAddParam = new PolicyUserAddParam();
        userAddParam.setUserId(userId);

        policyAddParam.setUsers(Collections.singletonList(userAddParam));
        return policyAddParam;
    }

    private PermissionAddParam createPermission(String name, List<Long> resourceIds, Long policyId) {
        PermissionAddParam permissionAddParam = new PermissionAddParam();
        permissionAddParam.setName(name);
        permissionAddParam.setLogic(Logic.ALL);
        permissionAddParam.setIntention(Intention.ALLOW);
        permissionAddParam.setIsEnabled(Boolean.TRUE);
        permissionAddParam.setResourceIds(resourceIds);
        permissionAddParam.setPolicyIds(Collections.singletonList(policyId));
        return permissionAddParam;
    }

    /**
     * ?????? realm ????????????
     */
    private String getRealmResourceTemplate() {
        if (realmResource == null) {
            synchronized (this) {
                if (realmResource == null) {
                    try {
                        File file = ResourceUtils.getFile("classpath:realm_resource.json");
                        realmResource = FileUtils.readFileToString(file, "utf-8");
                    } catch (IOException e) {
                        log.error("read realm_resource.json error", e);
                    }
                }
            }
        }
        return realmResource;
    }

    /**
     * ?????? client ????????????
     */
    private String getClientResourceTemplate() {
        if (clientResource == null) {
            synchronized (this) {
                if (clientResource == null) {
                    try {
                        File file = ResourceUtils.getFile("classpath:client_resource.json");
                        clientResource = FileUtils.readFileToString(file, "utf-8");
                    } catch (IOException e) {
                        log.error("read client_resource.json error", e);
                    }
                }
            }
        }
        return clientResource;
    }

    /**
     * ??????????????????doorkeeper???????????? ??????????????? doorkeeper?????????????????????
     *
     * @param userId ??????ID
     * @return ?????????????????????
     */
    public boolean isSuperAdmin(Long userId) {
        RolePO doorkeeperAdmin = roleMapper.getByName(getDoorkeeperRealmId(), DoorkeeperConstants.REALM_CLIENT_ID, DoorkeeperConstants.ADMIN);

        RoleCheckParam param = new RoleCheckParam();
        param.setRoleIds(Collections.singletonList(doorkeeperAdmin.getRoleId()));
        RoleCheckResult check = roleChecker.check(userId, param);
        return CollectionUtils.isNotEmpty(check.getRoles());
    }

    /**
     * ???????????????????????????
     */
    public boolean hasUriPermission(UserPO user, String path, UriMethod method) {
        // ????????????????????????doorkeeper???????????????
        if (!getDoorkeeperRealmId().equals(user.getRealmId())) {
            return false;
        }

        // ?????????????????????
        if (isSuperAdmin(user.getUserId())) {
            return true;
        }

        // ??????uri??????
        ClientPO apiClient = clientMapper.getByName(doorkeeperRealm.getRealmId(), DoorkeeperConstants.API_CLIENT);
        ResourceCheckParam checkParam = new ResourceCheckParam()
                .setClientId(apiClient.getClientId())
                .setUris(Collections.singletonList(method + DoorkeeperConstants.KV_DELIMITER + path));
        ResourceCheckResult checkResult = resourceChecker.check(user.getUserId(), checkParam);

        return checkResult.getResources().size() > 0;
    }

    public Long getDoorkeeperRealmId() {
        return getDoorkeeperRealm().getRealmId();
    }

    private RealmPO getDoorkeeperRealm() {
        if (doorkeeperRealm == null) {
            synchronized (this) {
                if (doorkeeperRealm == null) {
                    doorkeeperRealm = realmMapper.selectOne(Wrappers.<RealmPO>lambdaQuery()
                            .eq(RealmPO::getName, DoorkeeperConstants.DOORKEEPER_REALM_NAME)
                    );
                    Verifies.notNull(doorkeeperRealm, "doorkeeper????????????");
                }
            }
        }
        return doorkeeperRealm;
    }

}
