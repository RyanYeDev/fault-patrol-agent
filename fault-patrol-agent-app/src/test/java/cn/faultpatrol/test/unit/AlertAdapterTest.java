package cn.faultpatrol.test.unit;

import cn.faultpatrol.api.dto.AlertRequestDTO;
import cn.faultpatrol.trigger.http.alert.adapter.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 告警适配器模块单元测试
 */
public class AlertAdapterTest {

    private AlertmanagerWebhookAdapter alertmanagerAdapter;
    private GrafanaWebhookAdapter grafanaAdapter;
    private MicroserviceActuatorAdapter microserviceAdapter;
    private CloudEventsAlertAdapter cloudEventsAdapter;
    private StandardAlertAdapter standardAdapter;
    private AlertAdapterFactory adapterFactory;

    @Before
    public void setUp() {
        alertmanagerAdapter = new AlertmanagerWebhookAdapter();
        grafanaAdapter = new GrafanaWebhookAdapter();
        microserviceAdapter = new MicroserviceActuatorAdapter();
        cloudEventsAdapter = new CloudEventsAlertAdapter();
        standardAdapter = new StandardAlertAdapter();

        adapterFactory = new AlertAdapterFactory();
        ReflectionTestUtils.setField(adapterFactory, "adapters", List.of(
                alertmanagerAdapter, grafanaAdapter, microserviceAdapter, cloudEventsAdapter, standardAdapter
        ));
        ReflectionTestUtils.setField(adapterFactory, "standardAlertAdapter", standardAdapter);
    }

    @Test
    public void testAlertmanagerWebhookAdapter() {
        String alertmanagerJson = """
                {
                  "receiver": "webhook-sre",
                  "status": "firing",
                  "alerts": [
                    {
                      "status": "firing",
                      "labels": {
                        "alertname": "PodCrashLooping",
                        "severity": "critical",
                        "service": "order-service"
                      },
                      "annotations": {
                        "summary": "Pod order-service-7f8d restarted frequently",
                        "description": "Container killed by OOMKilled"
                      },
                      "generatorURL": "http://prometheus:9090/graph"
                    }
                  ],
                  "commonLabels": {
                    "alertname": "PodCrashLooping",
                    "severity": "critical",
                    "service": "order-service"
                  }
                }
                """;

        assertTrue(alertmanagerAdapter.supports("alertmanager", alertmanagerJson));
        assertTrue(alertmanagerAdapter.supports(null, alertmanagerJson));
        assertEquals("alertmanager", alertmanagerAdapter.getSourceType());

        AlertRequestDTO dto = alertmanagerAdapter.adapt(alertmanagerJson, Map.of());
        assertNotNull(dto);
        assertTrue(dto.getAlertName().contains("order-service"));
        assertTrue(dto.getAlertName().contains("PodCrashLooping"));
        assertEquals("critical", dto.getSeverity());
        assertEquals("prometheus-alertmanager", dto.getSource());
        assertTrue(dto.getAlertContent().contains("OOMKilled"));
    }

    @Test
    public void testGrafanaWebhookAdapter() {
        String grafanaJson = """
                {
                  "title": "High CPU Usage Alert",
                  "state": "alerting",
                  "message": "Node cpu usage exceeded 90%",
                  "ruleName": "CPU-Alert-Rule",
                  "ruleUrl": "http://grafana:3000/d/alert",
                  "evalMatches": [
                    {
                      "metric": "node_cpu_seconds_total",
                      "value": 94.5
                    }
                  ]
                }
                """;

        assertTrue(grafanaAdapter.supports("grafana", grafanaJson));
        assertTrue(grafanaAdapter.supports(null, grafanaJson));
        assertEquals("grafana", grafanaAdapter.getSourceType());

        AlertRequestDTO dto = grafanaAdapter.adapt(grafanaJson, Map.of());
        assertNotNull(dto);
        assertEquals("[unknown-service] CPU-Alert-Rule", dto.getAlertName());
        assertEquals("warning", dto.getSeverity());
        assertEquals("grafana", dto.getSource());
        assertTrue(dto.getAlertContent().contains("Node cpu usage exceeded 90%"));
        assertTrue(dto.getAlertContent().contains("http://grafana:3000/d/alert"));
    }

    @Test
    public void testMicroserviceActuatorAdapter() {
        String actuatorJson = """
                {
                  "serviceName": "payment-service",
                  "instanceId": "payment-service-192.168.1.10",
                  "status": "DOWN",
                  "reason": "Database connection pool exhausted",
                  "details": {
                    "db": "DOWN (Cannot get JDBC connection)",
                    "diskSpace": "UP"
                  },
                  "metrics": {
                    "activeConnections": 100,
                    "pendingRequests": 50
                  }
                }
                """;

        assertTrue(microserviceAdapter.supports("microservice", actuatorJson));
        assertTrue(microserviceAdapter.supports(null, actuatorJson));
        assertEquals("microservice", microserviceAdapter.getSourceType());

        AlertRequestDTO dto = microserviceAdapter.adapt(actuatorJson, Map.of());
        assertNotNull(dto);
        assertTrue(dto.getAlertName().contains("payment-service"));
        assertEquals("critical", dto.getSeverity());
        assertEquals("microservice-actuator", dto.getSource());
        assertTrue(dto.getAlertContent().contains("Database connection pool exhausted"));
        assertTrue(dto.getAlertContent().contains("activeConnections: 100"));
    }

    @Test
    public void testCloudEventsAlertAdapter() {
        String cloudEventsJson = """
                {
                  "specversion": "1.0",
                  "type": "org.cloud.monitoring.alert",
                  "source": "/k8s/cluster-east/namespaces/prod",
                  "id": "event-12345",
                  "time": "2026-09-20T00:00:00Z",
                  "data": {
                    "alertName": "KubernetesNodeNotReady",
                    "severity": "critical",
                    "content": "Node k8s-worker-02 state is NotReady"
                  }
                }
                """;

        assertTrue(cloudEventsAdapter.supports("cloudevents", cloudEventsJson));
        assertTrue(cloudEventsAdapter.supports(null, cloudEventsJson));

        AlertRequestDTO dto = cloudEventsAdapter.adapt(cloudEventsJson, Map.of());
        assertNotNull(dto);
        assertEquals("[/k8s/cluster-east/namespaces/prod] org.cloud.monitoring.alert", dto.getAlertName());
        assertEquals("warning", dto.getSeverity());
        assertEquals("cloudevents", dto.getSource());
        assertTrue(dto.getAlertContent().contains("Node k8s-worker-02 state is NotReady"));
    }

    @Test
    public void testStandardAlertAdapter() {
        String standardJson = """
                {
                  "alertName": "CustomDiskSpaceAlert",
                  "severity": "warning",
                  "source": "host-monitor",
                  "alertContent": "Disk /data space usage > 85%"
                }
                """;

        AlertRequestDTO dto = standardAdapter.adapt(standardJson, Map.of());
        assertNotNull(dto);
        assertEquals("CustomDiskSpaceAlert", dto.getAlertName());
        assertEquals("warning", dto.getSeverity());
        assertEquals("host-monitor", dto.getSource());
        assertEquals("Disk /data space usage > 85%", dto.getAlertContent());
    }

    @Test
    public void testAlertAdapterFactoryRouting() {
        // 1. Alertmanager 路径提示路由
        AlertRequestDTO dto1 = adapterFactory.adapt("alertmanager", "{\"status\":\"firing\",\"receiver\":\"webhook\"}", Map.of());
        assertEquals("prometheus-alertmanager", dto1.getSource());

        // 2. Grafana 自动探测路由
        String grafanaJson = "{\"title\":\"Lag Alert\",\"state\":\"alerting\",\"evalMatches\":[]}";
        AlertRequestDTO dto2 = adapterFactory.adapt(null, grafanaJson, Map.of());
        assertEquals("grafana", dto2.getSource());

        // 3. 微服务自动探测路由
        String microserviceJson = "{\"serviceName\":\"inventory\",\"status\":\"DOWN\"}";
        AlertRequestDTO dto3 = adapterFactory.adapt(null, microserviceJson, Map.of());
        assertEquals("microservice-actuator", dto3.getSource());

        // 4. 兜底普通文本
        AlertRequestDTO dto4 = adapterFactory.adapt(null, "这是一段纯文本告警信息，无特定格式", Map.of());
        assertEquals("custom-webhook", dto4.getSource());
        assertTrue(dto4.getAlertContent().contains("这是一段纯文本告警信息"));
    }

}
