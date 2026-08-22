/**
 * @author HXN
 * @date 2026-08-18 16:20
 * @description execution 模块包说明
 */
/**
 * M8/M9/M10 - 执行与报告模块
 *
 * <p>职责：测试套件、用例编排、执行调度、实时推送、报告分析
 *
 * <p>子包结构：
 * <ul>
 *   <li>controller - SuiteController, CaseController, PlanController, ExecutionController, AnalyticsController</li>
 *   <li>service - SuiteService, CaseService, ExecutionService, AnalyticsService, ReportService</li>
 *   <li>mapper - 各实体 Mapper</li>
 *   <li>entity - TestSuite, TestCase, TestPlan, Execution 等</li>
 *   <li>dto - 请求/响应 DTO</li>
 *   <li>engine - 执行引擎核心包（KeywordExecutor, HttpClientEngine, AssertionEngine 等）</li>
 *   <li>context - ExecutionContext（运行时变量上下文）</li>
 *   <li>mq - RabbitMQ 消息生产者/消费者</li>
 *   <li>websocket - WebSocket 实时推送</li>
 *   <li>config - AsyncConfig, WebSocketConfig</li>
 * </ul>
 */
package com.platform.execution;
