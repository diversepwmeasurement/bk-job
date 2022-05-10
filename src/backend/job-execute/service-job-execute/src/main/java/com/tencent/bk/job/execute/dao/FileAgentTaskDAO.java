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

package com.tencent.bk.job.execute.dao;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupBaseDTO;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;

import java.util.List;
import java.util.Map;

/**
 * FileAgentTaskDAO
 */
public interface FileAgentTaskDAO {
    /**
     * 批量新增/更新Agent任务
     *
     * @param agentTasks Agent任务列表
     */
    void batchSaveAgentTasks(List<AgentTaskDTO> agentTasks);

    /**
     * 批量更新Agent任务结果(status/startTime/endTime/totalTime)
     *
     * @param agentTasks Agent任务列表
     */
    void batchUpdateAgentTasks(List<AgentTaskDTO> agentTasks);

    /**
     * 获取步骤成功执行的Agent任务数量
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 步骤成功执行的Agent任务数量
     */
    int getSuccessAgentTaskCount(long stepInstanceId, int executeCount);

    /**
     * 根据GSE Agent 任务状态分组计数结果
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @return 根据GSE Agent 任务状态分组计数结果
     */
    Map<IpStatus, Integer> countStepAgentTaskGroupByStatus(long stepInstanceId, int executeCount);

    /**
     * 查询执行结果分组
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @return 执行结果分组
     */
    List<AgentTaskResultGroupBaseDTO> listResultGroups(long stepInstanceId, int executeCount, Integer batch);

    /**
     * 根据执行结果查询Agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param status         任务状态
     * @return Agent任务
     */
    List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  Integer status);

    /**
     * 根据执行结果查询Agent任务(排序、限制返回数量)
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param status         任务状态
     * @param limit          最大返回数量
     * @param orderField     排序字段
     * @param order          排序方式
     * @return Agent任务
     */
    List<AgentTaskDTO> listAgentTaskByResultGroup(Long stepInstanceId,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  Integer status,
                                                  Integer limit,
                                                  String orderField,
                                                  Order order);

    /**
     * 获取agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @param fileTaskMode   文件分发任务模式;传入null表示忽略该过滤条件
     * @return agent任务
     */
    List<AgentTaskDTO> listAgentTasks(Long stepInstanceId,
                                      Integer executeCount,
                                      Integer batch,
                                      FileTaskModeEnum fileTaskMode);

    /**
     * 根据GSE任务ID获取agent任务
     *
     * @param gseTaskId GSE任务ID
     * @return agent任务
     */
    List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId);

    /**
     * 根据IP查询Agent任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次；传入null或者0将忽略该参数
     * @param mode           文件分发任务模式
     * @param cloudIp        云区域+IP
     * @return Agent任务
     */
    AgentTaskDTO getAgentTaskByIp(Long stepInstanceId, Integer executeCount, Integer batch,
                                  FileTaskModeEnum mode, String cloudIp);

    /**
     * 获取任务目标ip - 根据ip模糊匹配
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param ipKeyword      用于检索的IP关键字
     * @return 匹配的任务目标IP列表
     */
    List<String> fuzzySearchTargetIpsByIp(Long stepInstanceId, Integer executeCount, String ipKeyword);

    /**
     * 获取文件任务源ip
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 文件任务源ip
     */
    List<String> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount);

    /**
     * 获取Agent任务实际执行成功的executeCount值(重试场景)
     *
     * @param stepInstanceId 步骤实例ID
     * @param batch          滚动执行批次；如果传入null或者0，忽略该参数
     * @param mode           文件分发任务模式
     * @param cloudIp        云区域+IP
     * @return Agent任务实际执行成功的executeCount值
     */
    int getActualSuccessExecuteCount(long stepInstanceId, Integer batch, FileTaskModeEnum mode, String cloudIp);

}
