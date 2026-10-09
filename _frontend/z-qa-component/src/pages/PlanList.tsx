import React, {useEffect, useState} from 'react';
import {Button, Card, Form, Input, message, Modal, Popconfirm, Select, Space, Table, Tag} from 'antd';
import {DeleteOutlined, EditOutlined, PlayCircleOutlined, PlusOutlined, ReloadOutlined, ScheduleOutlined} from '@ant-design/icons';
import {planApi, suiteApi} from '../services/api';
import {useNavigate} from 'react-router-dom';

/**
 * 测试计划管理.
 * 路径: /tools/qa/plans
 */
const PlanList: React.FC = () => {
    const [plans, setPlans] = useState<any[]>([]);
    const [suites, setSuites] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [modalOpen, setModalOpen] = useState(false);
    const [editing, setEditing] = useState<any>(null);
    const [form] = Form.useForm();
    const navigate = useNavigate();

    const load = async () => {
        setLoading(true);
        try {
            const [p, s] = await Promise.all([planApi.list(), suiteApi.list({})]);
            setPlans(p?.content || []);
            setSuites(s?.content || []);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
    }, []);

    const handleAdd = () => {
        setEditing(null);
        form.resetFields();
        form.setFieldsValue({status: 1, notify_type: 'inapp'});
        setModalOpen(true);
    };

    const handleEdit = (p: any) => {
        setEditing(p);
        // suite_ids/case_ids 是 JSON 字符串, 转回数组
        const values = {...p};
        try {
            values.suite_ids = JSON.parse(p.suite_ids || '[]');
        } catch {
            values.suite_ids = [];
        }
        try {
            values.case_ids = JSON.parse(p.case_ids || '[]');
        } catch {
            values.case_ids = [];
        }
        form.setFieldsValue(values);
        setModalOpen(true);
    };

    const handleSave = async () => {
        try {
            const v = await form.validateFields();
            // 数组转 JSON
            v.suite_ids = JSON.stringify(v.suite_ids || []);
            v.case_ids = JSON.stringify(v.case_ids || []);
            if (editing) {
                await planApi.update({...v, id: editing.id});
                message.success('更新成功');
            } else {
                await planApi.add(v);
                message.success('新增成功');
            }
            setModalOpen(false);
            load();
        } catch (e: any) {
            if (e?.errorFields) return;
            message.error('保存失败: ' + (e?.message || e));
        }
    };

    const handleRun = async (p: any) => {
        try {
            const ui = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const res = await planApi.run(p.id, p.env_code, ui.username || 'admin', 'manual');
            if (res?.code !== 200) {
                message.warning(res?.msg || '触发失败');
                return;
            }
            const run = res?.content;
            message.success(`已触发: ${run?.run_code}`);
            navigate(`/test/qa/runs/${run?.id}`);
        } catch (e: any) {
            message.error('触发失败: ' + (e?.message || e));
        }
    };

    const handleDelete = async (id: number) => {
        try {
            await planApi.remove(id);
            message.success('删除成功');
            load();
        } catch (e: any) {
            message.error('删除失败: ' + (e?.message || e));
        }
    };

    return (
        <div style={{padding: 0}}>
            <Card
                title={<Space><ScheduleOutlined/> 测试计划</Space>}
                extra={
                    <Space>
                        <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd}>新增计划</Button>
                    </Space>
                }
            >
                <Table
                    rowKey="id"
                    dataSource={plans}
                    loading={loading}
                    pagination={{pageSize: 20}}
                    columns={[
                        {title: 'ID', dataIndex: 'id', width: 60},
                        {title: 'Code', dataIndex: 'code', width: 200, render: t => <code>{t}</code>},
                        {title: '名称', dataIndex: 'name', width: 200},
                        {
                            title: 'Cron', dataIndex: 'cron_expr', width: 120,
                            render: t => t ? <Tag color="purple">{t}</Tag> : <Tag>手动</Tag>
                        },
                        {
                            title: '套件数', width: 100,
                            render: (_, p: any) => {
                                try {
                                    return JSON.parse(p.suite_ids || '[]').length;
                                } catch {
                                    return 0;
                                }
                            }
                        },
                        {
                            title: '通知', dataIndex: 'notify_type', width: 100,
                            render: t => t ? <Tag>{t}</Tag> : '-'
                        },
                        {
                            title: '状态', dataIndex: 'status', width: 80,
                            render: s => s === 1 ? <Tag color="success">启用</Tag> : <Tag>禁用</Tag>
                        },
                        {
                            title: '操作', width: 220,
                            render: (_, p) => (
                                <Space>
                                    <Button size="small" type="primary" icon={<PlayCircleOutlined/>}
                                            onClick={() => handleRun(p)}>运行</Button>
                                    <Button size="small" icon={<EditOutlined/>}
                                            onClick={() => handleEdit(p)}>编辑</Button>
                                    <Popconfirm title="确认删除?" onConfirm={() => handleDelete(p.id)}>
                                        <Button size="small" danger icon={<DeleteOutlined/>}>删除</Button>
                                    </Popconfirm>
                                </Space>
                            )
                        },
                    ]}
                />
            </Card>

            <Modal title={editing ? '编辑计划' : '新增计划'} open={modalOpen}
                   onCancel={() => setModalOpen(false)} onOk={handleSave} destroyOnHidden>
                <Form form={form} layout="vertical">
                    <Form.Item name="code" label="Code (唯一)" rules={[{required: true}, {pattern: /^[a-z0-9_-]+$/}]}>
                        <Input placeholder="如: nightly-regression"/>
                    </Form.Item>
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input placeholder="如: 夜间回归"/>
                    </Form.Item>
                    <Form.Item name="suite_ids" label="关联套件 (多选)">
                        <Select mode="multiple" placeholder="选择套件">
                            {suites.map(s => <Select.Option key={s.id}
                                                            value={s.id}>{s.code} — {s.name}</Select.Option>)}
                        </Select>
                    </Form.Item>
                    <Form.Item name="cron_expr" label="Cron 表达式 (空 = 手动)">
                        <Input placeholder="如: 0 0 2 * * ? (每天凌晨2点)"/>
                    </Form.Item>
                    <Form.Item name="notify_type" label="通知方式">
                        <Select>
                            <Select.Option value="none">none (不通知)</Select.Option>
                            <Select.Option value="inapp">inapp (站内消息)</Select.Option>
                            <Select.Option value="email">email (邮件)</Select.Option>
                            <Select.Option value="all">all (站内+邮件)</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="owner" label="负责人">
                        <Input placeholder="admin"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2}/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default PlanList;