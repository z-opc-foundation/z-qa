import React, {useMemo, useState} from 'react';
import {Button, Card, Form, Input, message, Modal, Popconfirm, Select, Space, Switch, Table, Tag} from 'antd';
import {EmptyState} from '@yuku123/z-frontend-common/ui';
import {DeleteOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons';
import {scheduleApi} from '../services/api';

/**
 * 定时执行 - 列表 + 新建 + 启停 + 删除.
 * 路径: /z-qa/schedules
 */
const ScheduleList: React.FC = () => {
    const [data, setData] = React.useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [open, setOpen] = React.useState(false);
    const [keyword, setKeyword] = useState('');
    const [form] = Form.useForm();

    const reload = async () => {
        setLoading(true);
        try {
            setData(((await scheduleApi.list()) as any).content || []);
        } finally {
            setLoading(false);
        }
    };

    React.useEffect(() => {
        reload();
    }, []);

    const filtered = useMemo(() => {
        const kw = keyword.trim().toLowerCase();
        if (!kw) return data;
        return data.filter((s) => [s.name, s.cron_expr, s.env_code, s.notify_channels]
            .some((v) => v && String(v).toLowerCase().includes(kw)));
    }, [data, keyword]);

    const handleDelete = async (id: number) => {
        try {
            await scheduleApi.remove(id);
            message.success('删除成功');
            reload();
        } catch (e: any) {
            message.error('删除失败: ' + (e?.message || e));
        }
    };

    const handleAdd = async () => {
        try {
            await scheduleApi.add(await form.validateFields());
            message.success('新建成功');
            setOpen(false);
            form.resetFields();
            reload();
        } catch (e: any) {
            if (e?.errorFields) return;
            message.error('新建失败: ' + (e?.message || e));
        }
    };

    return (
        <Card
            title="定时调度"
            extra={
                <Space>
                    <Input.Search
                        allowClear
                        placeholder="搜名称 / cron / 环境"
                        style={{width: 240}}
                        onChange={(e) => setKeyword(e.target.value)}
                    />
                    <Button onClick={async () => {
                        await scheduleApi.scanNow();
                        message.success('已扫描');
                        reload();
                    }}>立即扫描</Button>
                    <Button icon={<ReloadOutlined/>} onClick={reload}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => setOpen(true)}>新建定时</Button>
                </Space>
            }
        >
            <Table
                dataSource={filtered}
                rowKey="id"
                loading={loading}
                pagination={{
                    pageSize: 20,
                    showSizeChanger: true,
                    pageSizeOptions: ['10', '20', '50', '100'],
                    showTotal: (t) => `共 ${t} 条`,
                }}
                locale={{
                    emptyText: data.length === 0
                        ? <EmptyState title="还没有定时任务" description="新建一条 cron，到点自动跑套件或计划"/>
                        : <EmptyState title="没有匹配的定时任务" description="换个关键词再试"/>
                }}
                columns={[
                    {title: '名称', dataIndex: 'name'},
                    {
                        title: '类型', dataIndex: 'target_type', width: 80,
                        render: (v) => <Tag color={v === 'plan' ? 'purple' : 'blue'}>{v}</Tag>
                    },
                    {
                        title: 'Cron', dataIndex: 'cron_expr', width: 160,
                        render: (v) => <code style={{fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace'}}>{v || '-'}</code>
                    },
                    {title: '环境', dataIndex: 'env_code', width: 100},
                    {title: '通知', dataIndex: 'notify_channels', width: 120},
                    {title: '下次', dataIndex: 'next_run_time', width: 180},
                    {title: '上次', dataIndex: 'last_run_time', width: 180},
                    {
                        title: '结果', dataIndex: 'last_run_result', width: 100,
                        render: (v) => v === 'failed' ? <Tag color="red">失败</Tag> : <Tag color="green">{v || '-'}</Tag>
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
                        title: '操作', width: 90, render: (_, r) => (
                            <Popconfirm
                                title="确认删除该定时任务？"
                                description="删除后不再按 cron 自动触发。历史执行记录保留。此操作不可撤销。"
                                okText="删除"
                                okButtonProps={{danger: true}}
                                cancelText="取消"
                                onConfirm={() => handleDelete(r.id)}
                            >
                                <Button type="link" size="small" danger icon={<DeleteOutlined/>}>删除</Button>
                            </Popconfirm>
                        )
                    },
                ]}
            />
            <Modal open={open} onCancel={() => setOpen(false)} onOk={handleAdd} title="新建定时任务" destroyOnHidden>
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
