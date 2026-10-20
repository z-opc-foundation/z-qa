import React, {useMemo, useState} from 'react';
import {Button, Card, Form, Input, message, Modal, Popconfirm, Select, Space, Table, Tag} from 'antd';
import {EmptyState} from '@yuku123/z-frontend-common/ui';
import {DeleteOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons';
import {caseApi} from '../services/api';

/**
 * 用例库 - 列表 + 新增 + 删除.
 * 路径: /z-qa/cases
 */
const CaseList: React.FC = () => {
    const [data, setData] = React.useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [open, setOpen] = React.useState(false);
    const [keyword, setKeyword] = useState('');
    const [priority, setPriority] = useState<string | undefined>();
    const [form] = Form.useForm();

    const reload = async () => {
        setLoading(true);
        try {
            setData(((await caseApi.list({})) as any).content || []);
        } finally {
            setLoading(false);
        }
    };

    React.useEffect(() => {
        reload();
    }, []);

    const filtered = useMemo(() => {
        const kw = keyword.trim().toLowerCase();
        return data.filter((c) => {
            if (priority && c.priority !== priority) return false;
            if (!kw) return true;
            return [c.code, c.name, c.category, c.owner, c.tags].some(
                (v) => v && String(v).toLowerCase().includes(kw));
        });
    }, [data, keyword, priority]);

    const handleDelete = async (id: number) => {
        try {
            await caseApi.remove(id);
            message.success('删除成功');
            reload();
        } catch (e: any) {
            message.error('删除失败: ' + (e?.message || e));
        }
    };

    const handleAdd = async () => {
        try {
            await caseApi.add(await form.validateFields());
            message.success('新增成功');
            setOpen(false);
            form.resetFields();
            reload();
        } catch (e: any) {
            if (e?.errorFields) return;
            message.error('新增失败: ' + (e?.message || e));
        }
    };

    return (
        <Card
            title="用例库"
            extra={
                <Space>
                    <Input.Search
                        allowClear
                        placeholder="搜编码 / 名称 / 负责人"
                        style={{width: 240}}
                        onChange={(e) => setKeyword(e.target.value)}
                    />
                    <Select
                        allowClear
                        placeholder="优先级"
                        style={{width: 110}}
                        value={priority}
                        onChange={setPriority}
                        options={['P0', 'P1', 'P2', 'P3'].map((v) => ({value: v, label: v}))}
                    />
                    <Button icon={<ReloadOutlined/>} onClick={reload}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => setOpen(true)}>新增用例</Button>
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
                        ? <EmptyState title="用例库还是空的" description="点「新增用例」建第一条"/>
                        : <EmptyState title="没有匹配的用例" description="换个关键词或清掉优先级筛选"/>
                }}
                columns={[
                    {title: '编码', dataIndex: 'code', width: 160},
                    {title: '名称', dataIndex: 'name'},
                    {
                        title: '优先级', dataIndex: 'priority', width: 80,
                        render: (v) => <Tag color={v === 'P0' ? 'red' : v === 'P1' ? 'orange' : 'blue'}>{v || 'P1'}</Tag>
                    },
                    {title: '分类', dataIndex: 'category', width: 120},
                    {title: 'Owner', dataIndex: 'owner', width: 120},
                    {title: 'Tags', dataIndex: 'tags'},
                    {
                        title: '状态', dataIndex: 'status', width: 80,
                        render: (v) => v === 0 ? <Tag>禁用</Tag> : <Tag color="green">正常</Tag>
                    },
                    {
                        title: '操作', width: 90, render: (_, r) => (
                            <Popconfirm
                                title="确认删除该用例？"
                                description="删除后已引用它的计划将失去该用例。此操作不可撤销。"
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
            <Modal open={open} onCancel={() => setOpen(false)} onOk={handleAdd} title="新增用例" destroyOnHidden>
                <Form form={form} layout="vertical">
                    <Form.Item name="code" label="编码" rules={[{required: true}]}><Input/></Form.Item>
                    <Form.Item name="name" label="名称" rules={[{required: true}]}><Input/></Form.Item>
                    <Form.Item name="priority" label="优先级" initialValue="P1">
                        <Select options={[{value: 'P0'}, {value: 'P1'}, {value: 'P2'}, {value: 'P3'}]}/>
                    </Form.Item>
                    <Form.Item name="category" label="分类"><Input/></Form.Item>
                    <Form.Item name="owner" label="负责人"><Input/></Form.Item>
                    <Form.Item name="description" label="描述"><Input.TextArea rows={2}/></Form.Item>
                </Form>
            </Modal>
        </Card>
    );
};

export default CaseList;
