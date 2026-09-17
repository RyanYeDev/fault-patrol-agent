package cn.faultpatrol.test.domain;

import cn.faultpatrol.domain.agent.model.entity.ExecuteCommandEntity;
import cn.faultpatrol.domain.agent.service.execute.fixed.FixedAgentExecuteStrategy;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 *
 * 2025/9/13 15:39
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class FixedAgentExecuteStrategyTest {

    @Resource
    private FixedAgentExecuteStrategy fixedAgentExecuteStrategy;

    @Test
    public void test_execute() throws Exception {
        fixedAgentExecuteStrategy.execute(ExecuteCommandEntity.builder()
                .aiAgentId("6")
                .sessionId("10100101")
                .message("""
                        订单服务接口错误率突增、响应时间升高，请对订单服务执行故障巡检诊断，输出根因分析与处置建议。""")
                .build(), null);
    }

}
