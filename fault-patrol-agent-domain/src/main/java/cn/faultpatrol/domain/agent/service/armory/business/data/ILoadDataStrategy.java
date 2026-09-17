package cn.faultpatrol.domain.agent.service.armory.business.data;

import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;

/**
 * 装配数据加载策略接口
 * <p>
 * 装配链路第一步按命令类型（api / model / tool_mcp / prompt / advisor / client）
 * 选择对应的数据加载策略，将 MySQL 中的组件配置载入动态上下文，
 * 供后续装配节点（API → MCP → Model → Advisor → Client）消费。
 */
public interface ILoadDataStrategy {

    /**
     * 加载组件配置数据到动态上下文
     *
     * @param requestParameter 装配命令（含命令类型与组件 ID 列表）
     * @param dynamicContext   装配动态上下文
     */
    void loadData(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext);

}
