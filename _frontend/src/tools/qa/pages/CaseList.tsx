import React from 'react';
import {Button, Card, Form, Input, Modal, Select, Space, Table, Tag} from 'antd';
import {caseApi} from '../services/api';

const CaseList: React.FC = () => {
    const [data, setData] = React.useState<any[]>([]);
    const [open, setOpen] = React.useState(false);
    const [form] = Form.useForm();
    const reload = async () => setData(((await caseApi.list({})) as any).content || []);
    React.useEffect(() => {
        reload();
    }, []);

    return (
        <Card title="用例管理 (FEATURE052 §7.1)" extra={
            <Space>
                <Button type="primary" onClick={() => setOpen(true)}>新增用例</Button>
                <Button onClick={reload}>刷新</Button>
            </Space>
        }>
            <Table dataSource={data} rowKey="id" columns={[
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
                    title: '操作', width: 120, render: (_, r) => (
                        <Button danger size="small" onClick={async () => {
                            await caseApi.remove(r.id);
                            reload();
                        }}>删除</Button>
                    )
                },
            ]}/>
            <Modal open={open} onCancel={() => setOpen(false)} onOk={async () => {
                const v = await form.validateFields();
                await caseApi.add(v);
                setOpen(false);
                reload();
            }} title="新增用例">
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
