import React from 'react';
import {Button, Card, Form, Input, message, Select, Tabs, Tag} from 'antd';
import {ListTable} from '@yuku123/z-frontend-common/ui';
import {e2eApi} from '../services/api';

const E2ESessions: React.FC = () => {
    const [actions, setActions] = React.useState<any[]>([]);
    const [active, setActive] = React.useState<string | null>(null);
    const [form] = Form.useForm();

    return (
        <Card title="E2E 浏览器会话">
            <Tabs activeKey={active || 'open'} onChange={setActive}
                  items={[
                      {
                          key: 'open', label: '开启会话',
                          children: (
                              <Form form={form} layout="inline" onFinish={async (v) => {
                                  const res: any = await e2eApi.openSession({
                                      runId: v.runId, runDetailId: null,
                                      browserType: v.browser || 'chromium',
                                      targetUrl: v.targetUrl,
                                  });
                                  if (res.success) {
                                      message.success('开启成功, sessionId=' + res.content);
                                  } else message.error(res.msg);
                              }}>
                                  <Form.Item name="runId" label="runId"><Input type="number"/></Form.Item>
                                  <Form.Item name="targetUrl" label="目标 URL" rules={[{required: true}]}>
                                      <Input style={{width: 320}} placeholder="https://example.com/login"/>
                                  </Form.Item>
                                  <Form.Item name="browser" label="浏览器">
                                      <Select style={{width: 120}} defaultValue="chromium"
                                              options={[{value: 'chromium'}, {value: 'firefox'}, {value: 'webkit'}]}/>
                                  </Form.Item>
                                  <Form.Item><Button type="primary"
                                                     htmlType="submit">开启浏览器会话</Button></Form.Item>
                              </Form>
                          ),
                      },
                      {
                          key: 'list', label: '操作记录',
                          children: (
                              <div>
                                  <p>输入 sessionId 拉取操作记录：</p>
                                  <Form layout="inline" onFinish={async (v) => {
                                      const list = ((await e2eApi.listActions(v.sessionId)) as any).content || [];
                                      setActions(list);
                                  }}>
                                      <Form.Item name="sessionId" label="sessionId" rules={[{required: true}]}><Input type="number"/></Form.Item>
                                      <Form.Item><Button type="primary" htmlType="submit">查询</Button></Form.Item>
                                  </Form>
                                  {/* 操作记录表走基座 ListTable（lead 008 §2 规则 2/3/4）：分页 / 过滤 / EmptyState 三件套。
                                      原来这张表既没有分页（数据一多没法翻页），也没有过滤框。
                                      页签本身已经嵌在「E2E 浏览器会话」这张 Card 里，
                                      所以基座 Card 设成无边框，避免卡片套卡片。 */}
                                  <ListTable
                                      style={{marginTop: 12}}
                                      rows={actions}
                                      rowKey="id"
                                      searchKeys="action_type,selector,action_value"
                                      searchPlaceholder="按动作类型 / 选择器 / 值过滤"
                                      emptyTitle="还没有操作记录"
                                      emptyDescription="开启会话后执行动作，记录会出现在这里"
                                      cardProps={{variant: 'borderless', styles: {body: {padding: 0}}}}
                                      columns={[
                                          {title: '顺序', dataIndex: 'step_no', width: 60},
                                          {title: '动作', dataIndex: 'action_type', width: 100},
                                          {title: '选择器', dataIndex: 'selector'},
                                          {title: '值', dataIndex: 'action_value'},
                                          {
                                              title: '结果', dataIndex: 'result', width: 80,
                                              render: (v) => v === 'pass' ? <Tag color="green">{v}</Tag> :
                                                  <Tag color="red">{v}</Tag>
                                          },
                                          {title: '截图', dataIndex: 'screenshot_oss_key'},
                                          {title: '耗时', dataIndex: 'duration_ms', width: 80},
                                      ]}/>
                              </div>
                          ),
                      },
                      {
                          key: 'execute', label: '执行动作',
                          children: (
                              <Form layout="inline" onFinish={async (v) => {
                                  const res: any = await e2eApi.executeAction({
                                      sessionId: v.sessionId,
                                      stepNo: v.stepNo,
                                      actionType: v.actionType,
                                      selector: v.selector,
                                      value: v.value,
                                      description: v.description,
                                      screenshot: 1,
                                  });
                                  if (res.success) message.success('执行成功'); else message.error(res.msg);
                              }}>
                                  <Form.Item name="sessionId" label="sessionId" rules={[{required: true}]}><Input
                                      type="number"/></Form.Item>
                                  <Form.Item name="stepNo" label="序号" initialValue={1}><Input
                                      type="number"/></Form.Item>
                                  <Form.Item name="actionType" label="动作类型" rules={[{required: true}]}>
                                      <Select style={{width: 140}} defaultValue="click"
                                              options={[{value: 'navigate'}, {value: 'click'}, {value: 'fill'},
                                                  {value: 'select'}, {value: 'hover'},
                                                  {value: 'wait'}, {value: 'assert_text'}, {value: 'assert_visible'}]}/>
                                  </Form.Item>
                                  <Form.Item name="selector" label="选择器"><Input style={{width: 200}}/></Form.Item>
                                  <Form.Item name="value" label="值"><Input style={{width: 160}}/></Form.Item>
                                  <Form.Item name="description" label="描述"><Input style={{width: 200}}/></Form.Item>
                                  <Form.Item><Button type="primary" htmlType="submit">执行</Button></Form.Item>
                              </Form>
                          ),
                      },
                  ]}/>
        </Card>
    );
};

export default E2ESessions;
