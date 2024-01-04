/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.manage.model.query;

import com.tencent.bk.job.common.cc.model.BaseRuleDTO;
import com.tencent.bk.job.common.cc.model.PropertyFilterDTO;
import com.tencent.bk.job.common.cc.model.req.ListKubeContainerByTopoReq;
import com.tencent.bk.job.common.cc.model.req.Page;
import com.tencent.bk.job.common.cc.model.req.field.ContainerFields;
import com.tencent.bk.job.common.cc.model.req.field.PodFields;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.model.OrderCondition;
import com.tencent.bk.job.common.model.PageCondition;
import com.tencent.bk.job.manage.model.dto.KubeTopoNode;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ListContainerByTopologyNodesReq;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 容器查询
 */
@Getter
@ToString
public class ContainerQuery {
    private final Long bizId;

    private final List<Long> ids;

    private final List<KubeTopoNode> nodes;

    private final List<String> containerUIDs;

    private final List<String> containerNames;

    private final List<String> podNames;

    private final PageCondition pageCondition;

    private final OrderCondition orderCondition;


    private ContainerQuery(Builder builder) {
        bizId = builder.bizId;
        ids = builder.ids;
        nodes = builder.nodes;
        containerUIDs = builder.containerUIDs;
        containerNames = builder.containerNames;
        podNames = builder.podNames;
        pageCondition = builder.pageCondition;
        orderCondition = builder.orderCondition;
    }

    public static ContainerQuery fromListContainerByTopologyNodesReq(Long bizId,
                                                                     ListContainerByTopologyNodesReq req) {
        PageCondition pageCondition = null;
        if (req.getStart() != null && req.getPageSize() != null) {
            pageCondition = PageCondition.build(req.getStart(), req.getPageSize());
        }

        return ContainerQuery.builder()
            .bizId(bizId)
            .containerUIDs(req.getContainerUIDList())
            .containerNames(req.getContainerNameList())
            .podNames(req.getPodNameList())
            .nodes(CollectionUtils.isNotEmpty(req.getNodeList()) ?
                req.getNodeList().stream()
                    .filter(node -> !CcNodeTypeEnum.BIZ.getType().equals(node.getObjectId()))
                    .map(nodeVO -> new KubeTopoNode(nodeVO.getObjectId(), nodeVO.getInstanceId()))
                    .collect(Collectors.toList())
                : null)
            .pageCondition(pageCondition)
            .build();
    }


    public ListKubeContainerByTopoReq toListKubeContainerByTopoReq() {
        ListKubeContainerByTopoReq req = new ListKubeContainerByTopoReq();
        req.setBizId(bizId);

        setKubeNodeCondition(req);

        setContainerFilterIfNecessary(req);

        setPodFilterIfNecessary(req);

        setPageIfNecessary(req);

        return req;
    }

    private void setKubeNodeCondition(ListKubeContainerByTopoReq req) {
        if (CollectionUtils.isNotEmpty(nodes)) {
            req.setNodeIdList(nodes.stream().map(KubeTopoNode::toKubeNodeID).collect(Collectors.toList()));
        }
    }

    private void setContainerFilterIfNecessary(ListKubeContainerByTopoReq req) {
        if (isExistContainerPropCondition()) {
            PropertyFilterDTO containerFilter = new PropertyFilterDTO();
            containerFilter.setCondition("AND");

            if (CollectionUtils.isNotEmpty(ids)) {
                containerFilter.addRule(BaseRuleDTO.in(ContainerFields.ID, ids));
            }

            if (CollectionUtils.isNotEmpty(containerUIDs)) {
                containerFilter.addRule(BaseRuleDTO.in(ContainerFields.CONTAINER_UID, containerUIDs));
            }

            if (CollectionUtils.isNotEmpty(containerNames)) {
                containerFilter.addRule(BaseRuleDTO.in(ContainerFields.NAME, containerNames));
            }
            req.setContainerFilter(containerFilter);
        }
    }

    private boolean isExistContainerPropCondition() {
        return CollectionUtils.isNotEmpty(ids)
            || CollectionUtils.isNotEmpty(containerUIDs)
            || CollectionUtils.isNotEmpty(containerNames);
    }

    private void setPodFilterIfNecessary(ListKubeContainerByTopoReq req) {
        if (CollectionUtils.isNotEmpty(podNames)) {
            PropertyFilterDTO podFilter = new PropertyFilterDTO();
            podFilter.setCondition("AND");
            podFilter.addRule(BaseRuleDTO.in(PodFields.NAME, podNames));
            req.setPodFilter(podFilter);
        }
    }

    private void setPageIfNecessary(ListKubeContainerByTopoReq req) {
        if (pageCondition != null) {
            Page page = new Page();
            page.setStart(pageCondition.getStart());
            page.setLimit(pageCondition.getLength());
            page.setSort(ContainerFields.ID);
            req.setPage(page);
        }
    }

    public static Builder builder() {
        return Builder.builder();
    }


    public static final class Builder {
        private Long bizId;
        private List<Long> ids;
        private List<KubeTopoNode> nodes;
        private List<String> containerUIDs;
        private List<String> containerNames;
        private List<String> podNames;
        private PageCondition pageCondition;
        private OrderCondition orderCondition;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder bizId(Long bizId) {
            this.bizId = bizId;
            return this;
        }

        public Builder ids(List<Long> ids) {
            this.ids = ids;
            return this;
        }

        public Builder nodes(List<KubeTopoNode> nodes) {
            this.nodes = nodes;
            return this;
        }

        public Builder containerUIDs(List<String> containerUIDs) {
            this.containerUIDs = containerUIDs;
            return this;
        }

        public Builder containerNames(List<String> containerNames) {
            this.containerNames = containerNames;
            return this;
        }

        public Builder podNames(List<String> podNames) {
            this.podNames = podNames;
            return this;
        }

        public Builder pageCondition(PageCondition pageCondition) {
            this.pageCondition = pageCondition;
            return this;
        }

        public Builder orderCondition(OrderCondition orderCondition) {
            this.orderCondition = orderCondition;
            return this;
        }

        public ContainerQuery build() {
            return new ContainerQuery(this);
        }
    }
}
