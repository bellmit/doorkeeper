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
import org.limbo.doorkeeper.api.constants.DoorkeeperConstants;
import org.limbo.doorkeeper.api.model.param.add.ResourceAddParam;
import org.limbo.doorkeeper.api.model.param.add.ResourceTagAddParam;
import org.limbo.doorkeeper.api.model.param.add.ResourceUriAddParam;
import org.limbo.doorkeeper.api.model.param.query.ResourceQueryParam;
import org.limbo.doorkeeper.api.model.param.batch.ResourceBatchUpdateParam;
import org.limbo.doorkeeper.api.model.param.update.ResourceUpdateParam;
import org.limbo.doorkeeper.api.model.vo.PageVO;
import org.limbo.doorkeeper.api.model.vo.ResourceVO;
import org.limbo.doorkeeper.server.infrastructure.dao.PermissionResourceDao;
import org.limbo.doorkeeper.server.infrastructure.po.*;
import org.limbo.doorkeeper.server.infrastructure.mapper.*;
import org.limbo.doorkeeper.server.infrastructure.exception.ParamException;
import org.limbo.doorkeeper.server.infrastructure.utils.EnhancedBeanUtils;
import org.limbo.doorkeeper.server.infrastructure.utils.MyBatisPlusUtils;
import org.limbo.doorkeeper.server.infrastructure.utils.Verifies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Devil
 * @date 2021/1/5 4:59 ??????
 */
@Service
public class ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private PermissionResourceMapper permissionResourceMapper;

    @Autowired
    private ResourceUriMapper resourceUriMapper;

    @Autowired
    private ResourceTagMapper resourceTagMapper;

    @Autowired
    private PermissionResourceDao permissionResourceDao;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private UriMapper uriMapper;

    @Autowired
    private ResourceAssociationMapper resourceAssociationMapper;

    @Transactional
    public ResourceVO add(Long realmId, Long clientId, ResourceAddParam param) {
        ClientPO client = clientMapper.getById(realmId, clientId);
        Verifies.notNull(client, "??????????????????");

        ResourcePO resource = EnhancedBeanUtils.createAndCopy(param, ResourcePO.class);
        resource.setRealmId(client.getRealmId());
        resource.setClientId(client.getClientId());
        try {
            resourceMapper.insert(resource);
        } catch (DuplicateKeyException e) {
            throw new ParamException("???????????????");
        }

        // ??????uri
        batchSaveUri(resource.getResourceId(), resource.getRealmId(), resource.getClientId(), param.getUris());
        // ????????????
        batchSaveTag(resource.getResourceId(), resource.getRealmId(), resource.getClientId(), param.getTags());
        // ????????????????????????
        bindPermissions(resource.getResourceId(), param.getPermissionIds());
        // ??????????????????
        bindResourceAssociation(resource.getResourceId(), param.getParentIds(), param.getParentNames());

        return EnhancedBeanUtils.createAndCopy(resource, ResourceVO.class);
    }

    @Transactional
    public void update(Long realmId, Long clientId, Long resourceId, ResourceUpdateParam param) {
        ResourcePO resource = resourceMapper.getById(realmId, clientId, resourceId);
        Verifies.notNull(resource, "???????????????");

        try {
            // ??????
            resourceMapper.update(null, Wrappers.<ResourcePO>lambdaUpdate()
                    .set(StringUtils.isNotBlank(param.getName()) && !resource.getName().equals(param.getName()),
                            ResourcePO::getName, param.getName())
                    .set(param.getDescription() != null, ResourcePO::getDescription, param.getDescription())
                    .set(param.getIsEnabled() != null, ResourcePO::getIsEnabled, param.getIsEnabled())
                    .eq(ResourcePO::getResourceId, resourceId)
            );
        } catch (DuplicateKeyException e) {
            throw new ParamException("???????????????");
        }

        // ??????uri
        resourceUriMapper.delete(Wrappers.<ResourceUriPO>lambdaQuery()
                .eq(ResourceUriPO::getResourceId, resourceId)
        );
        // ??????uri
        batchSaveUri(resource.getResourceId(), resource.getRealmId(), resource.getClientId(), param.getUris());

        // ??????tag
        resourceTagMapper.delete(Wrappers.<ResourceTagPO>lambdaQuery()
                .eq(ResourceTagPO::getResourceId, resourceId)
        );
        // ??????tag
        batchSaveTag(resource.getResourceId(), resource.getRealmId(), resource.getClientId(), param.getTags());

        // ??????????????????
        resourceAssociationMapper.delete(Wrappers.<ResourceAssociationPO>lambdaQuery()
                .eq(ResourceAssociationPO::getResourceId, resourceId)
        );
        // ??????????????????
        bindResourceAssociation(resource.getResourceId(), param.getParentIds(), param.getParentNames());
    }

    @Transactional
    public void batchUpdate(Long realmId, Long clientId, ResourceBatchUpdateParam param) {
        switch (param.getType()) {
            case UPDATE:
                if (CollectionUtils.isEmpty(param.getResourceIds())) {
                    return;
                }
                resourceMapper.update(null, Wrappers.<ResourcePO>lambdaUpdate()
                        .set(param.getIsEnabled() != null, ResourcePO::getIsEnabled, param.getIsEnabled())
                        .in(ResourcePO::getResourceId, param.getResourceIds())
                        .eq(ResourcePO::getRealmId, realmId)
                        .eq(ResourcePO::getClientId, clientId)
                );
                break;
            case DELETE:
                if (CollectionUtils.isEmpty(param.getResourceIds())) {
                    return;
                }
                List<ResourcePO> resources = resourceMapper.selectList(Wrappers.<ResourcePO>lambdaQuery()
                        .select(ResourcePO::getResourceId)
                        .eq(ResourcePO::getRealmId, realmId)
                        .eq(ResourcePO::getClientId, clientId)
                        .in(ResourcePO::getResourceId, param.getResourceIds())
                );
                if (CollectionUtils.isEmpty(resources)) {
                    return;
                }
                List<Long> resourceIds = resources.stream().map(ResourcePO::getResourceId).collect(Collectors.toList());
                resourceMapper.deleteBatchIds(resourceIds);
                permissionResourceMapper.delete(Wrappers.<PermissionResourcePO>lambdaQuery()
                        .in(PermissionResourcePO::getResourceId, resourceIds)
                );
                resourceTagMapper.delete(Wrappers.<ResourceTagPO>lambdaQuery()
                        .in(ResourceTagPO::getResourceId, resourceIds)
                );
                resourceUriMapper.delete(Wrappers.<ResourceUriPO>lambdaQuery()
                        .in(ResourceUriPO::getResourceId, resourceIds)
                );
                resourceAssociationMapper.delete(Wrappers.<ResourceAssociationPO>lambdaQuery()
                        .in(ResourceAssociationPO::getResourceId, resourceIds)
                );
                resourceAssociationMapper.delete(Wrappers.<ResourceAssociationPO>lambdaQuery()
                        .in(ResourceAssociationPO::getParentId, resourceIds)
                );
                break;
            default:
                break;
        }
    }

    public ResourceVO get(Long realmId, Long clientId, Long resourceId) {
        return resourceMapper.getVO(realmId, clientId, resourceId);
    }

    public PageVO<ResourceVO> page(Long realmId, Long clientId, ResourceQueryParam param) {
        param.setRealmId(realmId);
        param.setClientId(clientId);
        long count = resourceMapper.voCount(param);

        PageVO<ResourceVO> result = PageVO.convertByPage(param);
        result.setTotal(count);
        if (count > 0) {
            result.setData(resourceMapper.getVOS(param));
        }
        return result;
    }

    /**
     * ????????????uri?????????resource_uri??????
     *
     * @param resourceId
     * @param realmId
     * @param clientId
     * @param params
     */
    private void batchSaveUri(Long resourceId, Long realmId, Long clientId, List<ResourceUriAddParam> params) {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }

        // ???????????????uri
        List<UriPO> uris = new ArrayList<>();
        HashSet<String> uriP = new HashSet<>();
        for (ResourceUriAddParam uriParam : params) {
            UriPO uri = new UriPO();
            uri.setRealmId(realmId);
            uri.setClientId(clientId);
            uri.setMethod(uriParam.getMethod());
            uri.setUri(uriParam.getUri().trim());

            uris.add(uri);
            uriP.add(uriParam.getUri().trim());
        }
        uriMapper.batchInsertIgnore(uris);

        // ?????? uri ????????????????????? id
        uris = uriMapper.selectList(Wrappers.<UriPO>lambdaQuery()
                .eq(UriPO::getRealmId, realmId)
                .eq(UriPO::getClientId, clientId)
                .in(UriPO::getUri, uriP)
        );

        List<ResourceUriPO> resourceUris = new ArrayList<>();
        for (ResourceUriAddParam uriParam : params) {
            for (UriPO uri : uris) {
                // ??????uri????????????????????? ???????????????????????????????????????????????????
                if (uri.getUri().trim().equals(uriParam.getUri().trim()) && uri.getMethod() == uriParam.getMethod()) {
                    ResourceUriPO resourceUri = new ResourceUriPO();
                    resourceUri.setResourceId(resourceId);
                    resourceUri.setUriId(uri.getUriId());
                    resourceUris.add(resourceUri);
                    break;
                }
            }
        }

        MyBatisPlusUtils.batchSave(resourceUris, ResourceUriPO.class);
    }

    /**
     * ????????????tag?????????resource_tag??????
     *
     * @param resourceId
     * @param realmId
     * @param clientId
     * @param params
     */
    private void batchSaveTag(Long resourceId, Long realmId, Long clientId, List<ResourceTagAddParam> params) {
        if (CollectionUtils.isEmpty(params)) {
            return;
        }

        // ?????????????????????
        List<TagPO> tags = new ArrayList<>();
        HashSet<String> kvs = new HashSet<>();
        for (ResourceTagAddParam tagParam : params) {
            TagPO tag = new TagPO();
            tag.setRealmId(realmId);
            tag.setClientId(clientId);
            tag.setK(tagParam.getK().trim());
            tag.setV(tagParam.getV().trim());
            String kv = tag.getK() + DoorkeeperConstants.KV_DELIMITER + tag.getV();
            tag.setKv(kv);

            tags.add(tag);
            kvs.add(kv);
        }
        tagMapper.batchInsertIgnore(tags);

        // ?????? tag ????????????????????? id
        tags = tagMapper.selectList(Wrappers.<TagPO>lambdaQuery()
                .eq(TagPO::getRealmId, realmId)
                .eq(TagPO::getClientId, clientId)
                .in(TagPO::getKv, kvs)
        );

        List<ResourceTagPO> resourceTags = new ArrayList<>();
        for (TagPO tag : tags) {
            ResourceTagPO resourceTag = new ResourceTagPO();
            resourceTag.setResourceId(resourceId);
            resourceTag.setTagId(tag.getTagId());
            resourceTags.add(resourceTag);
        }
        MyBatisPlusUtils.batchSave(resourceTags, ResourceTagPO.class);
    }


    private void bindPermissions(Long resourceId, List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        List<PermissionResourcePO> params = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            PermissionResourcePO addParam = new PermissionResourcePO();
            addParam.setPermissionId(permissionId);
            addParam.setResourceId(resourceId);
            params.add(addParam);
        }
        permissionResourceDao.batchSave(params);
    }

    /**
     * ????????????????????????
     *
     * @param resourceId
     * @param parentIds
     * @param parentNames
     */
    private void bindResourceAssociation(Long resourceId, List<Long> parentIds, List<String> parentNames) {
        if (CollectionUtils.isEmpty(parentIds)) {
            parentIds = new ArrayList<>();
        }

        // ?????? parentNames ??? ??????ID
        if (CollectionUtils.isNotEmpty(parentNames)) {
            List<ResourcePO> resources = resourceMapper.selectList(Wrappers.<ResourcePO>lambdaQuery()
                    .in(ResourcePO::getName, parentNames)
            );
            if (CollectionUtils.isNotEmpty(resources)) {
                parentIds.addAll(resources.stream().map(ResourcePO::getResourceId).collect(Collectors.toList()));
            }
        }

        List<ResourceAssociationPO> params = new ArrayList<>();
        for (Long parentId : parentIds) {
            if (resourceId.equals(parentId)) {
                // ??????????????????
                continue;
            }
            ResourceAssociationPO model = new ResourceAssociationPO();
            model.setParentId(parentId);
            model.setResourceId(resourceId);
            params.add(model);
        }
        if (CollectionUtils.isEmpty(params)) {
            return;
        }
        resourceAssociationMapper.batchInsertIgnore(params);

    }

}
