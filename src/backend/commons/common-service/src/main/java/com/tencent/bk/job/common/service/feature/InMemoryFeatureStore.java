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

package com.tencent.bk.job.common.service.feature;

import com.tencent.bk.job.common.service.feature.strategy.AllMatchToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.AnyMatchToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.FeatureConfigParseException;
import com.tencent.bk.job.common.service.feature.strategy.JobInstanceAttrToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.ResourceScopeBlackListToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.ResourceScopeWhiteListToggleStrategy;
import com.tencent.bk.job.common.service.feature.strategy.WeightToggleStrategy;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.feature.Feature;
import com.tencent.bk.job.common.util.feature.FeatureStore;
import com.tencent.bk.job.common.util.feature.ToggleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 特性开关配置存储实现
 */
@Slf4j
public class InMemoryFeatureStore implements FeatureStore {

    /**
     * key: featureId; value: Feature
     */
    private volatile Map<String, Feature> features = new HashMap<>();
    /**
     * 是否初始化
     */
    private volatile boolean isInitial = false;

    @Override
    public Feature getFeature(String featureId) {
        if (!isInitial) {
            synchronized (this) {
                if (!isInitial) {
                    load(false);
                }
            }
        }
        return features.get(featureId);
    }

    @Override
    public void load(boolean ignoreException) {
        synchronized (this) {
            log.info("Load feature toggle start ...");
            FeatureToggleConfig featureToggleConfig = ApplicationContextRegister.getBean(FeatureToggleConfig.class);

            if (featureToggleConfig.getFeatures() == null || featureToggleConfig.getFeatures().isEmpty()) {
                log.info("Feature toggle config empty!");
                return;
            }

            Map<String, Feature> tmpFeatures = new HashMap<>();
            featureToggleConfig.getFeatures().forEach((featureId, featureConfig) -> {
                try {
                    Feature feature = parseFeatureConfig(featureId, featureConfig);
                    tmpFeatures.put(featureId, feature);
                } catch (Throwable e) {
                    String msg = MessageFormatter.format(
                        "Load feature toggle config fail, skip update feature toggle config! featureId: {}, " +
                            "featureConfig: {}", featureId, featureConfig).getMessage();
                    log.error(msg, e);
                    if (features.get(featureId) != null) {
                        // 如果加载失败，那么使用原有的特性配置
                        tmpFeatures.put(featureId, features.get(featureId));
                    }
                }
            });

            // 使用新的配置完全替换老的配置
            features = tmpFeatures;
            log.info("Load feature toggle config done! features: {}", features);
            isInitial = true;
        }
    }

    private Feature parseFeatureConfig(String featureId,
                                       FeatureConfig featureConfig) throws FeatureConfigParseException {
        if (StringUtils.isBlank(featureId)) {
            log.error("FeatureId is blank");
            throw new FeatureConfigParseException("FeatureId is blank");
        }
        Feature feature = new Feature();
        feature.setId(featureId);
        feature.setEnabled(featureConfig.isEnabled());

        if (featureConfig.isEnabled()) {
            ToggleStrategyConfig strategyConfig = featureConfig.getStrategy();
            if (strategyConfig != null) {
                ToggleStrategy toggleStrategy = parseToggleStrategy(strategyConfig);
                if (toggleStrategy != null) {
                    feature.setStrategy(toggleStrategy);
                }
            }
        }
        return feature;
    }

    private ToggleStrategy parseToggleStrategy(ToggleStrategyConfig strategyConfig) {
        String strategyId = strategyConfig.getId();
        ToggleStrategy toggleStrategy;
        switch (strategyId) {
            case ResourceScopeWhiteListToggleStrategy.STRATEGY_ID:
                toggleStrategy = new ResourceScopeWhiteListToggleStrategy(strategyConfig.getDescription(),
                    strategyConfig.getParams());
                break;
            case ResourceScopeBlackListToggleStrategy.STRATEGY_ID:
                toggleStrategy = new ResourceScopeBlackListToggleStrategy(strategyConfig.getDescription(),
                    strategyConfig.getParams());
                break;
            case WeightToggleStrategy.STRATEGY_ID:
                toggleStrategy = new WeightToggleStrategy(strategyConfig.getDescription(),
                    strategyConfig.getParams());
                break;
            case JobInstanceAttrToggleStrategy.STRATEGY_ID:
                toggleStrategy = new JobInstanceAttrToggleStrategy(strategyConfig.getDescription(),
                    strategyConfig.getParams());
                break;
            case AllMatchToggleStrategy.STRATEGY_ID:
                toggleStrategy = new AllMatchToggleStrategy(
                    strategyId,
                    strategyConfig.getStrategies()
                        .stream()
                        .map(this::parseToggleStrategy)
                        .collect(Collectors.toList()),
                    strategyConfig.getParams());
                break;
            case AnyMatchToggleStrategy.STRATEGY_ID:
                toggleStrategy = new AnyMatchToggleStrategy(
                    strategyId,
                    strategyConfig.getStrategies()
                        .stream()
                        .map(this::parseToggleStrategy)
                        .collect(Collectors.toList()),
                    strategyConfig.getParams());
                break;
            default:
                log.error("Unsupported toggle strategy: {} , ignore it!", strategyId);
                throw new FeatureConfigParseException("Unsupported toggle strategy " + strategyId);
        }
        return toggleStrategy;
    }
}
