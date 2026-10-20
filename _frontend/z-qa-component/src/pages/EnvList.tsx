import React, {useEffect, useMemo, useState} from 'react';
import {Button, Card, Form, Input, InputNumber, message, Modal, Popconfirm, Space, Table, Tag} from 'antd';
import {EmptyState} from '@yuku123/z-frontend-common/ui';
import {DeleteOutlined, EditOutlined, GlobalOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons';
import {envApi} from '../services/api';

/**
 * 测试环境管理.
 * 路径: /z-qa/envs
 */
const EnvList: React.FC = () => {
    const [envs, setEnvs] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [modalOpen, setModalOpen] = useState(false);
    const [editing, setEditing] = useState<any>(null);
    const [keyword, setKeyword] = useState('');
    const [form] = Form.useForm();

    const load = async () => {
        setLoading(true);
        try {
            const res = await envApi.list();
            setEnvs(res?.content || []);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
    }, []);

    const filtered = useMemo(() => {
        const kw = keyword.trim().toLowerCase();
        if (!kw) return envs;
        return envs.filter((e) => [e.code, e.name, e.base_url, e.description]
            .some((v) => v && String(v).toLowerCase().includes(kw)));
    }, [envs, keyword]);

    const handleAdd = () => {
        setEditing(null);
        form.resetFields();
        form.setFieldsValue({priority: 0});
        setModalOpen(true);
    };

    const handleEdit = (e: any) => {
        setEditing(e);
        form.setFieldsValue(e);
        setModalOpen(true);
    };

    const handleSave = async () => {
        try {
            const v = await form.validateFields();
            if (editing) {
                await envApi.update({...v, id: editing.id});
                message.success('更新成功');
            } else {
                await envApi.add(v);
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
            await envApi.remove(id);
            message.success('删除成功');
            load();
        } catch (e: any) {
            message.error('删除失败: ' + (e?.message || e));
        }
    };

    return (
        <div style={{padding: 0}}>
            <Card
                title={<Space><GlobalOutlined/> 测试环境</Space>}
                extra={
                    <Space>
                        <Input.Search
                            allowClear
                            placeholder="搜 code / 名称 / baseUrl"
                            style={{width: 240}}
                            onChange={(e) => setKeyword(e.target.value)}
                        />
                        <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
                        <Button type="primary" icon={<PlusOutlined/>} onClick={handleAdd}>新增环境</Button>
                    </Space>
                }
            >
                <Table
                    rowKey="id"
                    dataSource={filtered}
                    loading={loading}
                    pagination={{
                        pageSize: 20,
                        showSizeChanger: true,
                        pageSizeOptions: ['10', '20', '50', '100'],
                        showTotal: (t) => `共 ${t} 条`,
                    }}
                    locale={{
                        emptyText: envs.length === 0
                            ? <EmptyState title="还没有测试环境" description="先建一个 local 或 staging，套件才有地方跑"/>
                            : <EmptyState title="没有匹配的环境" description="换个关键词再试"/>
                    }}
                    columns={[
                        {title: 'ID', dataIndex: 'id', width: 60},
                        {title: 'Code', dataIndex: 'code', width: 120, render: t => <Tag color="blue">{t}</Tag>},
                        {title: '名称', dataIndex: 'name', width: 180},
                        {
                            title: 'Base URL', dataIndex: 'base_url', ellipsis: true,
                            render: t => <code style={{fontSize: 11}}>{t}</code>
                        },
                        {
                            title: '默认 Headers', dataIndex: 'headers_json',
                            render: t => t ? <code style={{fontSize: 10}}>{t}</code> : '-'
                        },
                        {title: '优先级', dataIndex: 'priority', width: 90},
                        {title: '描述', dataIndex: 'description', ellipsis: true},
                        {
                            title: '操作', width: 160,
                            render: (_, e) => (
                                <Space>
                                    <Button type="link" size="small" icon={<EditOutlined/>}
                                            onClick={() => handleEdit(e)}>编辑</Button>
                                    <Popconfirm
                                        title="确认删除该环境？"
                                        description="删除后引用此环境的套件将无法执行。执行记录保留。此操作不可撤销。"
                                        okText="删除"
                                        okButtonProps={{danger: true}}
                                        cancelText="取消"
                                        onConfirm={() => handleDelete(e.id)}
                                    >
                                        <Button type="link" size="small" danger icon={<DeleteOutlined/>}>删除</Button>
                                    </Popconfirm>
                                </Space>
                            )
                        },
                    ]}
                />
            </Card>

            <Modal title={editing ? '编辑环境' : '新增环境'} open={modalOpen}
                   onCancel={() => setModalOpen(false)} onOk={handleSave} destroyOnHidden>
                <Form form={form} layout="vertical">
                    <Form.Item name="code" label="Code (唯一)" rules={[{required: true}, {pattern: /^[a-z0-9_-]+$/}]}>
                        <Input placeholder="如: staging"/>
                    </Form.Item>
                    <Form.Item name="name" label="名称" rules={[{required: true}]}>
                        <Input placeholder="如: 预发环境"/>
                    </Form.Item>
                    <Form.Item name="base_url" label="Base URL" rules={[{required: true}]}>
                        <Input placeholder="如: https://staging.example.com"/>
                    </Form.Item>
                    <Form.Item name="headers_json" label="默认 Headers (JSON)">
                        <Input.TextArea rows={2} placeholder='{"X-Env": "staging"}'/>
                    </Form.Item>
                    <Form.Item name="priority" label="优先级 (越大越靠前)">
                        <InputNumber min={0}/>
                    </Form.Item>
                    <Form.Item name="description" label="描述">
                        <Input.TextArea rows={2}/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default EnvList;