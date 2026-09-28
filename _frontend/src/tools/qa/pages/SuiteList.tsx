import React, {useEffect, useState} from 'react';
import {Button, Card, Form, Input, message, Modal, Popconfirm, Select, Space, Switch, Table, Tag, Tooltip} from 'antd';
import {
    DeleteOutlined,
    EditOutlined,
    EyeOutlined,
    FileTextOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    ReloadOutlined
} from '@ant-design/icons';
import {runApi, suiteApi} from '../services/api';
import {useNavigate} from 'react-router-dom';

/**
 * 测试套件管理 - 列表 + CRUD + 运行触发.
 * 路径: /tools/qa/suites
 */
const SuiteList: React.FC = () => {
    const [suites, setSuites] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [modalOpen, setModalOpen] = useState(false);
    const [editing, setEditing] = useState<any>(null);
    const [form] = Form.useForm();
    const navigate = useNavigate();

    const load = async () => {
        setLoading(true);
        try {
            const res = await suiteApi.list({});
            setSuites(res?.content || []);
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
        form.setFieldsValue({category: 'smoke', env_code: 'local', status: 1, version: 1, fail_fast: 0});
        setModalOpen(true);
    };

    const handleEdit = (s: any) => {
        setEditing(s);
        form.setFieldsValue(s);
        setModalOpen(true);
    };

    const handleSave = async () => {
        try {
            const values = await form.validateFields();
            if (editing) {
                await suiteApi.update({...values, id: editing.id});
                message.success('更新成功');
            } else {
                await suiteApi.add(values);
                message.success('新增成功');
            }
            setModalOpen(false);
            load();
        } catch (e: any) {
            if (e?.errorFields) return;
            message.error('保存失败: ' + (e?.message || e));
        }
    };

    const handleDelete = async (id: number) => {
        try {
            await suiteApi.remove(id);
            message.success('删除成功');
            load();
        } catch (e: any) {
            message.error('删除失败: ' + (e?.message || e));
        }
    };

    const handleRun = async (s: any) => {
        try {
            const ui = JSON.parse(localStorage.getItem('userInfo') || '{}');
            const res = await runApi.trigger(s.id, s.env_code, ui.username || 'admin', 'manual');
            const run = res?.content;
            message.success(`已触发: ${run?.run_code}`);
            navigate(`/test/qa/runs/${run?.id}`);
        } catch (e: any) {
            message.error('运行失败: ' + (e?.message || e));
        }
    };

    const handleViewSteps = (s: any) => {
        navigate(`/test/qa/suites/${s.id}/steps`, {state: {suite: s}});
    };

    const categoryColors: Record<string, string> = {
        smoke: 'orange', regression: 'red', api: 'blue', e2e: 'purple',
    };

    return (
        <div style={{padding: 24}}>
            <Card
                title={<Space><FileTextOutlined/> 测试套件管理</Space>}
                extra={
                    <Space>
                        <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd}>新增套件</Button>
                    </Space>
                }
            >
                <Table
                    rowKey="id"
                    dataSource={suites}
                    loading={loading}
                    pagination={{pageSize: 20}}
                    columns={[
                        {title: 'ID', dataIndex: 'id', width: 60},
                        {title: 'Code', dataIndex: 'code', width: 200, render: t => <code>{t}</code>},
                        {title: '名称', dataIndex: 'name', width: 200},
                        {
                            title: '分类', dataIndex: 'category', width: 100,
                            render: t => t ? <Tag color={categoryColors[t]}>{t}</Tag> : '-'
                        },
                        {
                            title: '环境', dataIndex: 'env_code', width: 100,
                            render: t => t ? <Tag>{t}</Tag> : '-'
                        },
                        {
                            title: '遇错即停', dataIndex: 'fail_fast', width: 90,
                            render: v => v === 1
                                ? <Tooltip title="本套件失败即跳过剩余步骤"><Tag color="volcano">开</Tag></Tooltip>
                                : <Tag>关</Tag>
                        },
                        {title: '负责人', dataIndex: 'owner', width: 100},
                        {title: '标签', dataIndex: 'tags', ellipsis: true},
                        {
                            title: '状态', dataIndex: 'status', width: 80,
                            render: s => s === 1 ? <Tag color="success">启用</Tag> : <Tag>禁用</Tag>
                        },
                        {
                            title: '操作', width: 280, fixed: 'right',
                            render: (_, s) => (
                                <Space>
                                    <Tooltip title="查看步骤">
                                        <Button size="small" icon={<EyeOutlined/>}
                                                onClick={() => handleViewSteps(s)}>步骤</Button>
                                    </Tooltip>
                                    <Button size="small" type="primary" icon={<PlayCircleOutlined/>}
                                            onClick={() => handleRun(s)}>运行</Button>
                                    <Button size="small" icon={<EditOutlined/>}
                                            onClick={() => handleEdit(s)}>编辑</Button>
                                    <Popconfirm title="确认删除?" onConfirm={() => handleDelete(s.id)}>
                                        <Button size="small" danger icon={<DeleteOutlined/>}>删除</Button>
                                    </Popconfirm>
                                </Space>
                            )
                        },
                    ]}
                />
            </Card>

            <Modal
                title={editing ? '编辑套件' : '新增套件'}
                open={modalOpen}
                onCancel={() => setModalOpen(false)}
                onOk={handleSave}
                destroyOnHidden
                size="large"
            >
                <Form form={form} layout="vertical">
                    <Form.Item name="code" label="Code (唯一)" rules={[{required: true}, {pattern: /^[a-z0-9_-]+$/}]}>
                        <Input placeholder="如: zopc-self-test"/>
                    </Form.Item>
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input placeholder="如: z-opc 自身集成测试"/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2}/>
                    </Form.Item>
                    <Form.Item name="category" label="分类">
                        <Select>
                            <Select.Option value="smoke">smoke (冒烟)</Select.Option>
                            <Select.Option value="regression">regression (回归)</Select.Option>
                            <Select.Option value="api">api (接口)</Select.Option>
                            <Select.Option value="e2e">e2e (浏览器)</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="env_code" label="默认环境">
                        <Select>
                            <Select.Option value="local">local</Select.Option>
                            <Select.Option value="prod">prod</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="owner" label="负责人">
                        <Input placeholder="admin"/>
                    </Form.Item>
                    <Form.Item name="tags" label="标签">
                        <Input placeholder="逗号分隔, 如: z-opc, smoke, ci"/>
                    </Form.Item>
                    <Form.Item
                        name="fail_fast"
                        label="遇错即停"
                        tooltip="本套件某步失败后跳过该套件剩余步骤；计划里的后续套件照常执行"
                        getValueProps={(v) => ({checked: v === 1})}
                        getValueFromEvent={(checked) => (checked ? 1 : 0)}
                    >
                        <Switch/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default SuiteList;