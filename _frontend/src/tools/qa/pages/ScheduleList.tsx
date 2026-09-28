import React from 'react';
import {Button, Card, Form, Input, message, Modal, Select, Space, Switch, Table, Tag} from 'antd';
import {scheduleApi} from '../services/api';

const ScheduleList: React.FC = () => {
    const [data, setData] = React.useState<any[]>([]);
    const [open, setOpen] = React.useState(false);
    const [form] = Form.useForm();
    const reload = async () => setData(((await scheduleApi.list()) as any).content || []);
    React.useEffect(() => {
        reload();
    }, []);

    return (
        <Card title="定时执行 (FEATURE052 Phase 5)" extra={
            <Space>
                <Button onClick={async () => {
                    await scheduleApi.scanNow();
                    message.success('已扫描');
                    reload();
                }}>立即扫描</Button>
                <Button type="primary" onClick={() => setOpen(true)}>新建定时</Button>
                <Button onClick={reload}>刷新</Button>
            </Space>
        }>
            <Table dataSource={data} rowKey="id" columns={[
                {title: '名称', dataIndex: 'name'},
                {
                    title: '类型', dataIndex: 'target_type', width: 80,
                    render: (v) => <Tag color={v === 'plan' ? 'purple' : 'blue'}>{v}</Tag>
                },
                // @ts-ignore
                {title: 'Cron', dataIndex: 'cron_expr', width: 160, fontFamily: 'monospace'},
                {title: '环境', dataIndex: 'env_code', width: 100},
                {title: '通知', dataIndex: 'notify_channels', width: 120},
                {title: '下次', dataIndex: 'next_run_time', width: 180},
                {title: '上次', dataIndex: 'last_run_time', width: 180},
                {
                    title: '结果', dataIndex: 'last_run_result', width: 100,
                    render: (v) => v === 'failed' ? <Tag color="red">失败</Tag> : <Tag color="green">{v}</Tag>
                },
                {
                    title: '启用', dataIndex: 'enabled', width: 80,
                    render: (v, r) => (
                        <Switch checked={v === 1} onChange={async (c) => {
                            await scheduleApi.toggle(r.id, c);
                            reload();
                        }}/>
                    )
                },
                {
                    title: '操作', width: 100, render: (_, r) => (
                        <Button danger size="small" onClick={async () => {
                            await scheduleApi.remove(r.id);
                            reload();
                        }}>删除</Button>
                    )
                },
            ]}/>
            <Modal open={open} onCancel={() => setOpen(false)} size="large" onOk={async () => {
                const v = await form.validateFields();
                await scheduleApi.add(v);
                setOpen(false);
                reload();
            }} title="新建定时任务">
                <Form form={form} layout="vertical">
                    <Form.Item name="name" label="名称" rules={[{required: true}]}><Input/></Form.Item>
                    <Form.Item name="target_type" label="类型" initialValue="suite" rules={[{required: true}]}>
                        <Select options={[{value: 'suite', label: '单套件'}, {value: 'plan', label: '批量计划'}]}/>
                    </Form.Item>
                    <Form.Item name="suite_id" label="套件 ID（type=suite 时填）"><Input type="number"/></Form.Item>
                    <Form.Item name="plan_id" label="计划 ID（type=plan 时填）"><Input type="number"/></Form.Item>
                    <Form.Item name="cron_expr" label="Cron 表达式 (如 0 0 23 * * ?)" rules={[{required: true}]}>
                        <Input placeholder="0 0 23 * * ?"/>
                    </Form.Item>
                    <Form.Item name="env_code" label="环境编码"><Input placeholder="local/prod"/></Form.Item>
                    <Form.Item name="notify_channels" label="通知渠道 (逗号分隔)">
                        <Input placeholder="in_app,email"/>
                    </Form.Item>
                    <Form.Item name="notify_on_pass" label="通过也通知" initialValue={0}
                                getValueProps={(v) => ({checked: v === 1})}
                                getValueFromEvent={(checked) => (checked ? 1 : 0)}>
                        <Switch/>
                    </Form.Item>
                    <Form.Item name="notify_on_fail" label="失败通知" initialValue={1}
                                getValueProps={(v) => ({checked: v === 1})}
                                getValueFromEvent={(checked) => (checked ? 1 : 0)}>
                        <Switch/>
                    </Form.Item>
                </Form>
            </Modal>
        </Card>
    );
};

export default ScheduleList;
