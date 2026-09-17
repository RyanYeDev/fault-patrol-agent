package cn.faultpatrol.domain.agent.service.armory.node;

import cn.faultpatrol.domain.agent.adapter.repository.IAgentRepository;
import cn.faultpatrol.domain.agent.model.entity.ArmoryCommandEntity;
import cn.faultpatrol.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import cn.faultpatrol.types.design.framework.tree.AbstractStrategyRouter;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;

/**
 * 装配支撑类：提供 Bean 动态注册等公共能力
 */
public abstract class AbstractArmorySupport extends AbstractStrategyRouter<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> {

    private final Logger log = LoggerFactory.getLogger(AbstractArmorySupport.class);

    @Resource
    protected ApplicationContext applicationContext;

    @Resource
    protected IAgentRepository repository;

    protected String beanName(String id) {
        return null;
    }

    protected String dataName() {
        return null;
    }

    /**
     * 通用的Bean注册方法
     *
     * @param beanName  Bean名称
     * @param beanClass Bean类型
     * @param <T>       Bean类型
     */
    protected synchronized <T> void registerBean(String beanName, Class<T> beanClass, T beanInstance) {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        // 注册Bean
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClass, () -> beanInstance);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        // 禁用destroy方法推断，防止Spring对AutoCloseable bean自动调用close()（McpSyncClient连接会被关闭）
        beanDefinition.setDestroyMethodName("");

        // 如果Bean已存在，先移除
        if (beanFactory.containsBeanDefinition(beanName)) {
            beanFactory.removeBeanDefinition(beanName);
        }

        // 注册新的Bean
        beanFactory.registerBeanDefinition(beanName, beanDefinition);

        log.info("成功注册Bean: {}", beanName);
    }

    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

}
